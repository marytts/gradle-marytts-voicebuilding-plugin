package de.dfki.mary.voicebuilding

import de.dfki.mary.ComponentPlugin

import de.dfki.mary.voicebuilding.tasks.GenerateVoiceConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.WriteProperties

class VoicebuildingBasePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(ComponentPlugin)
        project.plugins.apply(MavenPublishPlugin)

        project.sourceCompatibility = '1.8'

        project.marytts.extensions.create('voice', VoiceExtension, project)
        project.marytts.voice.extensions.create('license', VoiceLicenseExtension, project)

        project.marytts {
            component {
                name = project.marytts.voice.nameCamelCase
                packageName = "marytts.voice.${project.marytts.component.name}"
                configBaseClass = "VoiceConfig"
            }
        }

        project.dependencies {
            runtimeOnly group: 'de.dfki.mary', name: "marytts-lang-$project.marytts.voice.language", version: project.marytts.version, {
                exclude group: '*', module: 'groovy-all'
                exclude group: 'com.twmacinta', module: 'fast-md5'
                exclude group: 'gov.nist.math', module: 'Jampack'
            }
            testImplementation group: 'junit', name: 'junit', version: '4.13.2'
            integrationTestImplementation localGroovy()
            integrationTestImplementation group: 'org.testng', name: 'testng', version: '7.5'
        }

        project.tasks.register('generateVoiceConfig', GenerateVoiceConfig) {
            group = 'MaryTTS Voicebuilding Base'
        }

        project.tasks.named('unpackTestSourceTemplates').configure {
            resourceNames.add 'VoiceConfigTest.java'
        }

        project.tasks.named('unpackIntegrationTestSourceTemplates').configure {
            resourceNames.add 'LoadVoiceIT.groovy'
        }

        project.tasks.named('generateConfig').configure {
            dependsOn project.tasks.named("generateVoiceConfig")
        }

        project.publishing {
            publications {
                mavenJava(MavenPublication) {
                    from project.components.java
                    project.afterEvaluate {
                        pom {
                            description = project.marytts.voice.description
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

        project.tasks.register('generatePomProperties', WriteProperties) {
            group = 'MaryTTS Voicebuilding Base'
            outputFile = project.layout.buildDirectory.file('pom.properties')
            properties = [
                    groupId   : project.group,
                    artifactId: project.name,
                    version   : project.version
            ]
        }

        project.tasks.named('processResources').configure {
            from project.generatePomProperties, {
                rename { "META-INF/maven/$project.group/voice-$project.marytts.voice.name/pom.xml" }
            }
            from project.generatePomProperties, {
                rename { "META-INF/maven/$project.group/voice-$project.marytts.voice.name/pom.properties" }
            }
        }

        project.tasks.register('run', JavaExec) {
            group = 'MaryTTS Voicebuilding Base'
            classpath = project.configurations.runtimeClasspath + project.sourceSets.main.output
            mainClass.set 'marytts.server.Mary'
            systemProperty 'log4j.logger.marytts', 'INFO,stderr'
        }
    }
}
