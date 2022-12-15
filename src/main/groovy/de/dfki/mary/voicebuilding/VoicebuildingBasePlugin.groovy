package de.dfki.mary.voicebuilding

import de.dfki.mary.ComponentPlugin

import de.dfki.mary.voicebuilding.tasks.GenerateVoiceSource
import de.dfki.mary.voicebuilding.tasks.GenerateVoiceConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.WriteProperties
import org.gradle.api.tasks.testing.Test

class VoicebuildingBasePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply JavaLibraryPlugin
        project.plugins.apply GroovyPlugin
        project.plugins.apply MavenPublishPlugin
        project.plugins.apply ComponentPlugin

        project.sourceCompatibility = '1.8'

        project.marytts.extensions.create 'voice', VoiceExtension, project
        project.marytts.voice.extensions.create 'license', VoiceLicenseExtension, project

        project.marytts {
            component {
                name = project.marytts.voice.nameCamelCase
                packageName = "marytts.voice.${project.marytts.component.name}"
                configBaseClass = "VoiceConfig"
            }
        }

        project.dependencies {
            api group: 'de.dfki.mary', name: "marytts-lang-$project.marytts.voice.language", version: project.marytts.version, {
                exclude group: '*', module: 'groovy-all'
                exclude group: 'com.twmacinta', module: 'fast-md5'
                exclude group: 'gov.nist.math', module: 'Jampack'
            }
            testImplementation group: 'junit', name: 'junit', version: '4.13.2'
            integrationTestImplementation localGroovy()
            integrationTestImplementation group: 'org.testng', name: 'testng', version: '7.5'
        }

        project.tasks.register 'generateVoiceConfig', GenerateVoiceConfig, {

        }

        project.tasks.register 'generateVoiceSource', GenerateVoiceSource, {
            dependsOn "generateSource", "generateConfig"
            testDirectory = project.file("$project.buildDir/generatedSrc/test/groovy/voice")
            integrationTestDirectory = project.file("$project.buildDir/generatedSrc/integrationTest/groovy/voice")

        }

        project.generateConfig {
            dependsOn project.tasks.named("generateVoiceConfig")
        }

        project.sourceSets {
            test {
                groovy {
                    srcDirs += project.generateVoiceSource.testDirectory.get()
                }
            }
            integrationTest {
                groovy {
                    srcDirs += project.generateVoiceSource.integrationTestDirectory.get()
                }
            }
        }


        project.publishing {
            publications {
                mavenJava(MavenPublication) {
                    from project.components.java
                    pom {
                        description = project.marytts.voice.description
                        // TODO: Properties not being resolved lazily in nested license extension, so...
                        project.afterEvaluate {
                            licenses {
                                license {
                                    name = project.marytts.voice.license.name
                                    url = project.marytts.voice.license.url
                                }
                            }
                        }
                    }
                }
            }
        }

        project.task('generatePomProperties', type: WriteProperties) {
            outputFile = project.layout.buildDirectory.file('pom.properties')
            properties = [
                    groupId   : project.group,
                    artifactId: project.name,
                    version   : project.version
            ]
        }

        project.processResources {
            from project.generatePomProperties, {
                rename { "META-INF/maven/$project.group/voice-$project.marytts.voice.name/pom.xml" }
            }
            from project.generatePomProperties, {
                rename { "META-INF/maven/$project.group/voice-$project.marytts.voice.name/pom.properties" }
            }
        }

        project.task('run', type: JavaExec) {
            classpath = project.configurations.runtimeClasspath + project.sourceSets.main.output
            mainClass = 'marytts.server.Mary'
            systemProperty 'log4j.logger.marytts', 'INFO,stderr'
        }
    }
}
