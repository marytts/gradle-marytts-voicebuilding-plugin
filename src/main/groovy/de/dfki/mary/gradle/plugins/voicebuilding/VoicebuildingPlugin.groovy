package de.dfki.mary.gradle.plugins.voicebuilding

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

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
    def maryLocale
    def maryLocaleXml
    def region
    def samplingRate
    def type
    def wantsToBeDefault

    VoicebuildingPluginVoiceExtension(final Project project) {
        this.project = project
    }

    def getLocale() {
        locale = locale ?: [language, getRegion()].join('_')
    }

    def getLocaleXml() {
        localeXml = localeXml ?: [language, getRegion()].join('-')
    }

    def getMaryLocale() {
        maryLocale = maryLocale ?: language.equalsIgnoreCase(getRegion()) ? language : getLocale()
    }

    def getMaryLocaleXml() {
        maryLocaleXml = maryLocaleXml ?: language.equalsIgnoreCase(getRegion()) ? language : getLocaleXml()
    }

    def getNameCamelCase() {
        nameCamelCase = nameCamelCase ?: name.split(/[^_A-Za-z0-9]/).collect { it.capitalize() }.join()
    }

    def getRegion() {
        region = region ?: language.toUpperCase()
    }
}

class VoicebuildingPluginLicenseExtension {
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

        project.sourceCompatibility = JavaVersion.VERSION_1_7

        voice = project.extensions.create 'voice', VoicebuildingPluginVoiceExtension, project
        license = project.extensions.create 'license', VoicebuildingPluginLicenseExtension
        project.ext {
            maryttsVersion = '5.1'
            maryttsRepoUrl = 'https://oss.jfrog.org/artifactory/libs-release/'
            generatedSrcDir = "$project.buildDir/generated-src"
            generatedTestSrcDir = "$project.buildDir/generated-test-src"
        }

        project.repositories.jcenter()
        project.repositories.maven {
            url project.maryttsRepoUrl
        }

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

        project.jar {
            manifest {
                attributes('Created-By': "${System.properties['java.version']} (${System.properties['java.vendor']})",
                        'Built-By': System.properties['user.name'],
                        'Built-With': "gradle-${project.gradle.gradleVersion}, groovy-${GroovySystem.version}")
            }
        }

        project.afterEvaluate {
            project.dependencies {
                compile "de.dfki.mary:marytts-lang-$voice.language:$project.maryttsVersion"
                testCompile "junit:junit:4.11"
            }
            if (voice.type == 'unit selection') {
                project.apply from: 'weights.gradle'
            }

            addTasks(project)
        }
    }

    private void addTasks(Project project) {

        project.task('generateSource', type: Copy) {
            from project.file(getClass().getResource("$templateDir/Config.java"))
            into project.generatedSrcDir
            rename {
                "marytts/voice/$project.voice.nameCamelCase/$it"
            }
            expand project.properties
        }

        project.compileJava {
            dependsOn 'generateSource'
        }

        project.task('generateTestSource', type: Copy) {
            from project.file(getClass().getResource("$templateDir/ConfigTest.java"))
            from project.file(getClass().getResource("$templateDir/LoadVoiceIT.java"))
            into project.generatedTestSrcDir
            rename {
                "marytts/voice/$project.voice.nameCamelCase/$it"
            }
            expand project.properties
        }

        project.compileTestJava {
            dependsOn 'generateTestSource'
        }

        project.task('generateServiceLoader') {
            def serviceLoaderFile = project.file("$project.sourceSets.main.output.resourcesDir/META-INF/services/marytts.config.MaryConfig")
            outputs.files serviceLoaderFile
            doFirst {
                project.file(serviceLoaderFile.parent).mkdirs()
            }
            doLast {
                serviceLoaderFile.text = "marytts.voice.${voice.nameCamelCase}.Config"
            }
        }

        project.task('generateVoiceConfig', type: Copy) {
            from project.file(getClass().getResource("$templateDir/voice${project.voice.type == "hsmm" ? "-hsmm" : ""}.config"))
            into project.sourceSets.main.output.resourcesDir
            rename {
                "marytts/voice/$project.voice.nameCamelCase/voice.config"
            }
            expand project.properties
        }

        project.task('generateFeatureFiles') {
            def destDir = "$project.sourceSets.main.output.resourcesDir/marytts/voice/$voice.nameCamelCase"
            def featureFile = project.file("$destDir/halfphoneUnitFeatureDefinition_ac.txt")
            def joinCostFile = project.file("$destDir/joinCostWeights.txt")
            outputs.files featureFile, joinCostFile
            doFirst {
                project.file(destDir).mkdirs()
            }
            doLast {
                def fpm
                try {
                    fpm = Class.forName("marytts.language.${voice.language}.features.FeatureProcessorManager").newInstance()
                } catch (e) {
                    logger.info "Reflection failed: $e"
                    logger.info "Instiating generic FeatureProcessorManager for locale $project.voice.maryLocale"
                    fpm = new marytts.features.FeatureProcessorManager(project.voice.maryLocale)
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

        project.processResources {
            rename {
                "marytts/voice/$project.voice.nameCamelCase/$it"
            }
            dependsOn 'generateServiceLoader', 'generateVoiceConfig'
            if (project.voice.type == 'unit selection') {
                dependsOn 'generateFeatureFiles'
            }
        }

        project.processDataResources {
            rename {
                "lib/voices/$voice.name/$it"
            }
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

        project.jar {
            dependsOn 'generatePom'
        }

        project.task('legacyComponentZip', type: Zip, dependsOn: ['jar', 'processDataResources']) {
            from project.sourceSets.data.output.resourcesDir
            from project.tasks['jar'].outputs.files, {
                rename {
                    "lib/$it"
                }
            }
        }

        project.task('legacyComponentXml', dependsOn: 'legacyComponentZip') << {
            project.ext {
                zipFile = project.tasks['legacyComponentZip'].outputs.files.singleFile
                zipFileHash = DigestUtils.md5Hex(new FileInputStream(zipFile))
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
