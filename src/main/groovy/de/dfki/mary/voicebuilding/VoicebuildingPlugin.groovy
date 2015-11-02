package de.dfki.mary.voicebuilding

import groovy.json.JsonBuilder
import groovy.xml.*

import marytts.features.FeatureProcessorManager

import org.apache.commons.codec.digest.DigestUtils

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

class VoicebuildingPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply JavaPlugin
        project.plugins.apply MavenPlugin
        project.plugins.apply VoicebuildingDataPlugin

        project.sourceCompatibility = JavaVersion.VERSION_1_7

        project.ext {
            generatedSrcDir = "$project.buildDir/generated-src"
            generatedTestSrcDir = "$project.buildDir/generated-test-src"
        }

        project.status = project.version.endsWith('SNAPSHOT') ? 'integration' : 'release'

        project.sourceSets {
            main {
                java {
                    srcDir project.generatedSrcDir
                }
            }
            data
            test {
                java {
                    srcDir project.generatedTestSrcDir
                }
                compileClasspath += data.output
            }
        }

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

        project.task('legacyComponentZip', type: Zip) {
            from project.processDataResources
            from(project.jar) {
                rename {
                    "lib/$it"
                }
            }
        }

        project.task('dataZip', type: Zip) {
            from project.processDataResources
            classifier 'data'
        }
    }

    private void addTasks(Project project) {

        project.task('generateServiceLoader') {
            def serviceLoaderFile = project.file("$project.sourceSets.main.output.resourcesDir/META-INF/services/marytts.config.MaryConfig")
            outputs.files serviceLoaderFile
            doFirst {
                serviceLoaderFile.parentFile.mkdirs()
            }
            doLast {
                serviceLoaderFile.text = "marytts.voice.${project.voice.nameCamelCase}.Config"
            }
        }

        project.task('generateVoiceConfig', type: Copy) {
            from project.templates
            into project.sourceSets.main.output.resourcesDir
            include project.voice.type == 'hsmm' ? 'voice-hsmm.config' : 'voice.config'
            rename {
                "marytts/voice/$project.voice.nameCamelCase/voice.config"
            }
            expand project.properties
        }

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
            dependsOn project.generateServiceLoader, project.generateVoiceConfig
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

        project.task('generatePom') {
            def pomDir = project.file("${project.sourceSets.main.output.resourcesDir}/META-INF/maven/${project.group.replace '.', '/'}/$project.name")
            def pomFile = project.file("$pomDir/pom.xml")
            def propFile = project.file("$pomDir/pom.properties")
            outputs.files project.files(pomFile, propFile)
            doFirst {
                pomDir.mkdirs()
            }
            doLast {
                project.pom { pom ->
                    pom.project {
                        description project.voice.description
                        licenses {
                            license {
                                name project.voice.license.name
                                url project.voice.license.url
                            }
                        }
                    }
                }.writeTo(pomFile)
                propFile.withWriter { dest ->
                    dest.println "version=$project.version"
                    dest.println "groupId=$project.group"
                    dest.println "artifactId=$project.name"
                }
            }
        }

        project.jar.dependsOn project.generatePom

        project.task('legacyComponentXml') {
            dependsOn project.legacyComponentZip
            def zipFile = project.legacyComponentZip.outputs.files.singleFile
            def xmlFile = project.file("$project.distsDir/$project.name-$project.version-component-descriptor.xml")
            inputs.files zipFile
            outputs.files xmlFile
            doLast {
                def zipFileHash = DigestUtils.md5Hex(new FileInputStream(zipFile))
                def builder = new StreamingMarkupBuilder()
                def xml = builder.bind {
                    'marytts-install'(xmlns: 'http://mary.dfki.de/installer') {
                        voice(gender: project.voice.gender, locale: project.voice.maryLocale, name: project.voice.name, type: project.voice.type, version: project.version) {
                            delegate.description project.voice.description
                            license(href: project.voice.license.url)
                            'package'(filename: zipFile.name, md5sum: zipFileHash, size: zipFile.size()) {
                                location(folder: true, href: "http://mary.dfki.de/download/$project.maryttsVersion/")
                            }
                            depends(language: project.voice.maryLocaleXml, version: project.maryttsVersion)
                        }
                    }
                }
                xmlFile.text = XmlUtil.serialize(xml)
            }
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

        project.task('visualizeTaskDependencyGraph') << {
            project.file('build.dot').withWriter { dot ->
                dot.println 'digraph G {'
                dot.println 'rankdir=BT;'
                dot.println 'node [shape=box];'
                dot.println 'edge [dir="back"];'
                project.tasks.each { task ->
                    dot.println "$task.name;"
                    task.taskDependencies.getDependencies(task).each { otherTask ->
                        dot.println "$task.name -> $otherTask.name;"
                    }
                }
                dot.println "}"
            }
        }
    }
}
