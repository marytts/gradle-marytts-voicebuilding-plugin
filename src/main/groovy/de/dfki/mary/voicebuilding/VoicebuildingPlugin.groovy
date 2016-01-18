package de.dfki.mary.voicebuilding

import groovy.json.JsonBuilder

import marytts.features.FeatureProcessorManager

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip

class VoicebuildingPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply VoicebuildingDataPlugin

        project.status = project.version.endsWith('SNAPSHOT') ? 'integration' : 'release'

        project.jar.manifest {
            attributes('Created-By': "${System.properties['java.version']} (${System.properties['java.vendor']})",
                    'Built-By': System.properties['user.name'],
                    'Built-With': "gradle-${project.gradle.gradleVersion}, groovy-${GroovySystem.version}")
        }

        project.afterEvaluate {

            addTasks(project)

            project.artifacts {
                if (project.voice.type == 'unit selection') {
                    archives project.dataZip
                }
            }
        }

        project.task('dataZip', type: Zip) {
            from project.processDataResources
            classifier 'data'
        }
    }

    private void addTasks(Project project) {

        project.task('generateFeatureFiles') {
            def destDir = project.file("$project.sourceSets.main.output.resourcesDir/marytts/voice/$project.voice.nameCamelCase")
            def featureFile = new File(destDir, 'halfphoneUnitFeatureDefinition_ac.txt')
            def joinCostFile = new File(destDir, 'joinCostWeights.txt')
            outputs.files featureFile, joinCostFile
            doFirst {
                destDir.mkdirs()
            }
            doLast {
                try {
                    project.apply from: 'weights.gradle'
                    def fpm
                    try {
                        fpm = Class.forName("marytts.language.${project.voice.language}.features.FeatureProcessorManager").newInstance()
                    } catch (e) {
                        logger.info "Reflection failed: $e"
                        logger.info "Instantiating generic FeatureProcessorManager for locale $project.voice.maryLocale"
                        fpm = new FeatureProcessorManager(project.voice.maryLocale)
                    }
                    featureFile.withWriter { dest ->
                        dest.println 'ByteValuedFeatureProcessors'
                        fpm.listByteValuedFeatureProcessorNames().tokenize().sort { a, b ->
                            if (a == 'halfphone_unitname') return -1
                            if (b == 'halfphone_unitname') return 1
                            a <=> b
                        }.each { name ->
                            def weight = project.featureWeights[name] ?: 0
                            def values = fpm.getFeatureProcessor(name).values.join(' ')
                            dest.println "$weight | $name $values"
                        }
                        dest.println 'ShortValuedFeatureProcessors'
                        fpm.listShortValuedFeatureProcessorNames().tokenize().each { name ->
                            def weight = project.featureWeights[name] ?: 0
                            def values = fpm.getFeatureProcessor(name).values.join(' ')
                            dest.println "$weight | $name $values"
                        }
                        dest.println 'ContinuousFeatureProcessors'
                        fpm.listContinuousFeatureProcessorNames().tokenize().each { name ->
                            def weight = project.featureWeights[name] ?: 0
                            dest.println "$weight | $name"
                        }
                    }
                    joinCostFile.withWriter { dest ->
                        (0..13).each { name ->
                            def weight = project.featureWeights[name] ?: '1.0 linear'
                            dest.println "${name.toString().padRight(2)} : $weight"
                        }
                    }
                } catch (e) {
                    logger.warn "No weights definition found -- assuming resources are provided..."
                }
            }
        }

        project.processResources {
            rename {
                "marytts/voice/$project.voice.nameCamelCase/$it"
            }
            if (project.voice.type == 'unit selection') {
                dependsOn project.generateFeatureFiles
            }
        }

        project.processDataResources.rename {
            "lib/voices/$project.voice.name/$it"
        }

        project.test {
            systemProperty 'mary.base', project.sourceSets.data.output.resourcesDir
            systemProperty 'log4j.logger.marytts', 'DEBUG,stderr'
            maxHeapSize = '1g'
        }

        project.task('generateJsonDescriptor') {
            def jsonFile = new File(project.distsDir, "$project.name-${project.version}.json")
            inputs.files project.uploadArchives.artifacts
            outputs.files jsonFile
            doFirst {
                project.distsDir.mkdirs()
            }
            doLast {
                def json = new JsonBuilder()
                json {
                    'group' project.group
                    'artifact' project.name
                    'version' project.version
                    'name' project.voice.name
                    'language' project.voice.language
                    'gender' project.voice.gender
                    'type' project.voice.type
                    'description' project.voice.description
                    'license' {
                        'name' project.voice.license.name
                        'url' project.voice.license.url
                    }
                    'files' inputs.files.collectEntries {
                        ant.checksum(file: it, algorithm: 'SHA-1', property: "${it.name}.sha1")
                        [(it.name): ['size': it.size(),
                                     'sha1': ant.properties["${it.name}.sha1"]]]
                    }
                }
                jsonFile.text = json.toPrettyString()
            }
        }
    }
}
