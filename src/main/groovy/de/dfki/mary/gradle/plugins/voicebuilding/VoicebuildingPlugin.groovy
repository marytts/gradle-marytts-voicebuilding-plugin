package de.dfki.mary.gradle.plugins.voicebuilding

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

import marytts.features.FeatureProcessorManager

import org.apache.commons.codec.digest.DigestUtils

class VoicebuildingPlugin implements Plugin<Project> {
    final templateDir = "/de/dfki/mary/gradle/plugins/voicebuilding/templates"

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)
        project.plugins.apply(MavenPlugin)

        project.sourceCompatibility = 1.7

        project.ext {
            voiceNameCamelCase = project.voiceName.split(/[^_A-Za-z0-9]/).collect { it.capitalize() }.join()
            generatedSrcDir = "$project.buildDir/generated-src"
            generatedTestSrcDir = "$project.buildDir/generated-test-src"
            generatedResourceDir = "$project.buildDir/generated-resources"
            voiceRegion = project.hasProperty('voiceRegion') ? voiceRegion : voiceLanguage.toUpperCase()
            voiceLocale = "${voiceLanguage}_$voiceRegion"
            voiceLocaleXml = "$voiceLanguage-$voiceRegion"
        }

        if (project.voiceType == 'unit selection') {
            project.apply from: 'weights.gradle'
        }

        project.sourceSets {
            main {
                java {
                    srcDir project.generatedSrcDir
                }
            }
            test {
                java {
                    srcDir project.generatedTestSrcDir
                }
            }
        }

        project.jar {
            manifest {
                attributes('Created-By': "${System.properties['java.version']} (${System.properties['java.vendor']})",
                        'Built-By': System.properties['user.name'],
                        'Built-With': "gradle-${project.gradle.gradleVersion}, groovy-${GroovySystem.version}")
            }
        }

        addTasks(project)
    }

    private void addTasks(Project project) {
        project.task('generateSource', type: Copy) {
            from project.file(getClass().getResource("$templateDir/Config.java"))
            into project.generatedSrcDir
            expand project.properties
            rename { "marytts/voice/$project.voiceNameCamelCase/$it" }
        }
        project.tasks['compileJava'].dependsOn 'generateSource'

        project.task('generateTestSource', type: Copy) {
            from project.file(getClass().getResource("$templateDir/ConfigTest.java"))
            from project.file(getClass().getResource("$templateDir/LoadVoiceIT.java"))
            into project.generatedTestSrcDir
            expand project.properties
            rename { "marytts/voice/$project.voiceNameCamelCase/$it" }
        }
        project.tasks['compileTestJava'].dependsOn 'generateTestSource'

        project.task('generateData', type: Copy) {
            from "data"
            into project.generatedResourceDir
        }

        project.processResources {
            filesMatching('voice.config') {
                expand project.properties
            }
            rename {
                "marytts/voice/$project.voiceNameCamelCase/$it"
            }
            exclude 'component-descriptor.xml'
            if (project.voiceType == "unit selection") {
                dependsOn 'generateData', 'generateFeatureFiles'
                ext.jarResourceNames = ['cart.mry',
                                        'dur.tree',
                                        'f0.left.tree',
                                        'f0.mid.tree',
                                        'f0.right.tree',
                                        'halfphoneUnitFeatureDefinition_ac.txt',
                                        'joinCostWeights.txt']
                from project.fileTree(project.generatedResourceDir) {
                    include jarResourceNames
                }
                rename {
                    "marytts/voice/$project.voiceNameCamelCase/$it"
                }
            }
            doLast {
                // generate voice config
                project.copy {
                    from project.file(getClass().getResource("$templateDir/voice${project.voiceType == "hsmm" ? "-hsmm" : ""}.config"))
                    into "$destinationDir/marytts/voice/$project.voiceNameCamelCase"
                    expand project.properties
                }
                // generate service loader
                project.copy {
                    from project.file(getClass().getResource("$templateDir/marytts.config.MaryConfig"))
                    into "$destinationDir/META-INF/services"
                    expand project.properties
                }
            }
        }

        if (project.voiceType == 'unit selection') {

            project.task('generateFeatureFiles', dependsOn: 'generateData') {
                def featureFile = project.file("$project.generatedResourceDir/halfphoneUnitFeatureDefinition_ac.txt")
                def joinCostFile = project.file("$project.generatedResourceDir/joinCostWeights.txt")
                outputs.files project.files(featureFile, joinCostFile)
                doLast {
                    def fpm
                    try {
                        fpm = new FeatureProcessorManager(project.voiceLocale)
                    } catch (e) {
                        fpm = new FeatureProcessorManager(project.voiceLanguage)
                    }
                    featureFile.withWriter { dest ->
                        dest.println 'ByteValuedFeatureProcessors'
                        fpm.listByteValuedFeatureProcessorNames().tokenize().sort { a, b ->
                            if (a == 'halfphone_unitname') return -1
                            if (b == 'halfphone_unitname') return 1
                            a <=> b
                        }.each { name ->
                            def weight = project.featureWeights[name] ?: 0
                            def values = fpm.getFeatureProcessor(name).getValues().join(' ')
                            dest.println "$weight | $name $values"
                        }
                        dest.println 'ShortValuedFeatureProcessors'
                        fpm.listShortValuedFeatureProcessorNames().tokenize().each { name ->
                            def weight = project.featureWeights[name] ?: 0
                            def values = fpm.getFeatureProcessor(name).getValues().join(' ')
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
                }
            }

            project.task('packageData', type: Zip, dependsOn: 'generateData') {
                from project.fileTree(project.generatedResourceDir) {
                    include 'halfphoneFeatures_ac.mry',
                            'halfphoneUnits.mry',
                            'joinCostFeatures.mry',
                            'timeline_basenames.mry',
                            'timeline_waveforms.mry'
                }
                rename {
                    "voices/$project.voiceName/$it"
                }
                classifier 'data'
            }

            project.artifacts {
                archives project.tasks['jar'], project.tasks['packageData']
            }
        }

        project.task('generatePom') {
            def groupAsPathString = project.group.replace('.', '/')
            def pomDir = project.file("${project.tasks['processResources'].destinationDir}/META-INF/maven/$groupAsPathString/$project.name")
            def pomFile = project.file("$pomDir/pom.xml")
            def propFile = project.file("$pomDir/pom.properties")
            outputs.files project.files(pomFile, propFile)
            doLast {
                pomDir.mkdirs()
                project.pom { pom ->
                    pom.project {
                        description project.voiceDescription
                        licenses {
                            license {
                                name project.licenseName
                                url project.licenseUrl
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
        project.tasks['jar'].dependsOn 'generatePom'

        project.task('legacyComponentZip', type: Zip, dependsOn: 'jar') {
            from(project.tasks['jar'].outputs.files) {
                rename {
                    "lib/$it"
                }
            }
            if (project.voiceType == 'unit selection') {
                dependsOn 'generateData'
                from(project.tasks['generateData'].outputs.files) {
                    rename {
                        "lib/voices/$project.voiceName/$it"
                    }
                    exclude project.tasks['processResources'].jarResourceNames
                }
            }
        }

        project.task('legacyComponentZipInfo', dependsOn: 'legacyComponentZip') << {
            def zipFile = project.tasks['legacyComponentZip'].outputs.files.singleFile
            ext.fileName = zipFile.name
            ext.fileSize = zipFile.size()
            def fis = new FileInputStream(zipFile)
            ext.fileHash = DigestUtils.md5Hex(fis)
        }

        project.task('legacyComponentXml', type: Copy, dependsOn: 'legacyComponentZipInfo') {
            from project.sourceSets.main.resources
            include 'component-descriptor.xml'
            rename {
                "$project.name-$version-$it"
            }
            into project.distsDir
            expand project.properties
        }
    }
}
