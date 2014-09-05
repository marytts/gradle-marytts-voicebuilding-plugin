package de.dfki.mary.gradle.plugins.voicebuilding

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

import marytts.features.FeatureProcessorManager

import org.apache.commons.codec.digest.DigestUtils

class VoicebuildingPluginVoiceExtension {
    final Project project

    def description
    def gender
    def name
    def nameCamelCase
    def language
    def locale
    def localeXml
    def region
    def samplingRate
    def type
    def wantsToBeDefault

    VoicebuildingPluginVoiceExtension(final Project project) {
        this.project = project
    }

    def getLocale(glue = '_') {
        locale = locale ?: [language, getRegion()].join(glue)
    }

    def getLocaleXml() {
        localeXml = localeXml ?: getLocale('-')
    }

    def getNameCamelCase() {
        nameCamelCase = nameCamelCase ?: name.split(/[^_A-Za-z0-9]/).collect { it.capitalize() }.join()
    }

    def getRegion() {
        region = region ?: language.toUpperCase()
    }
}

class VoicebuildinfPluginLicenseExtension {
    def name
    def shortName
    def url
}

class VoicebuildingPlugin implements Plugin<Project> {
    final templateDir = "/de/dfki/mary/gradle/plugins/voicebuilding/templates"
    def voice
    def license

    @Override
    void apply(Project project) {
        project.plugins.apply JavaPlugin
        project.plugins.apply MavenPlugin

        project.sourceCompatibility = 1.7

        voice = project.extensions.create 'voice', VoicebuildingPluginVoiceExtension, project
        license = project.extensions.create 'license', VoicebuildinfPluginLicenseExtension
        project.ext {
            maryttsVersion = '5.1'
            maryttsRepoUrl = 'http://oss.jfrog.org/artifactory/libs-release/'
            generatedSrcDir = "$project.buildDir/generated-src"
            generatedTestSrcDir = "$project.buildDir/generated-test-src"
            generatedResourceDir = "$project.buildDir/generated-resources"
        }

        project.repositories.jcenter()
        project.repositories.maven {
            url project.maryttsRepoUrl
        }

        project.afterEvaluate {
            project.dependencies {
                compile "de.dfki.mary:marytts-lang-$voice.language:$project.maryttsVersion"
            }
            if (voice.type == 'unit selection') {
                project.apply from: 'weights.gradle'
            }
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
            rename { "marytts/voice/$project.voice.nameCamelCase/$it" }
        }
        project.tasks['compileJava'].dependsOn 'generateSource'

        project.task('generateTestSource', type: Copy) {
            from project.file(getClass().getResource("$templateDir/ConfigTest.java"))
            from project.file(getClass().getResource("$templateDir/LoadVoiceIT.java"))
            into project.generatedTestSrcDir
            expand project.properties
            rename { "marytts/voice/$project.voice.nameCamelCase/$it" }
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
                "marytts/voice/$project.voice.nameCamelCase/$it"
            }
            exclude 'component-descriptor.xml'
            project.afterEvaluate {
                if (project.voice.type == "unit selection") {
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
                        "marytts/voice/$project.voice.nameCamelCase/$it"
                    }
                }
            }
            doLast {
                // generate voice config
                project.copy {
                    from project.file(getClass().getResource("$templateDir/voice${project.voice.type == "hsmm" ? "-hsmm" : ""}.config"))
                    into "$destinationDir/marytts/voice/$project.voice.nameCamelCase"
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

        project.afterEvaluate {
            if (project.voice.type == 'unit selection') {

                project.task('generateFeatureFiles', dependsOn: 'generateData') {
                    def featureFile = project.file("$project.generatedResourceDir/halfphoneUnitFeatureDefinition_ac.txt")
                    def joinCostFile = project.file("$project.generatedResourceDir/joinCostWeights.txt")
                    outputs.files project.files(featureFile, joinCostFile)
                    doLast {
                        def fpm
                        try {
                            fpm = new FeatureProcessorManager(project.voice.locale)
                        } catch (e) {
                            fpm = new FeatureProcessorManager(project.voice.language)
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
                        "voices/$project.voice.name/$it"
                    }
                    classifier 'data'
                }

                project.artifacts {
                    archives project.tasks['jar'], project.tasks['packageData']
                }
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
                        description voice.description
                        licenses {
                            license {
                                name project.license.name
                                url project.license.url
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
            project.afterEvaluate {
                if (project.voice.type == 'unit selection') {
                    dependsOn 'generateData'
                    from(project.tasks['generateData'].outputs.files) {
                        rename {
                            "lib/voices/$voice.name/$it"
                        }
                        exclude project.tasks['processResources'].jarResourceNames
                    }
                }
            }
        }

        project.task('legacyComponentXml', dependsOn: 'legacyComponentZip') << {
            def zipFile = project.tasks['legacyComponentZip'].outputs.files.singleFile
            project.ext {
                fileName = zipFile.name
                fileSize = zipFile.size()
                fileHash = DigestUtils.md5Hex(new FileInputStream(zipFile))
            }
            project.copy {
                from project.file(getClass().getResource("$templateDir/component-descriptor.xml"))
                into project.distsDir
                rename {
                    "$project.name-$project.version-$it"
                }
                expand project.properties
            }
        }
    }
}
