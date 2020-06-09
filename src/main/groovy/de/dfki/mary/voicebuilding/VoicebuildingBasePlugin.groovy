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

        project.repositories {
            jcenter()
        }

        project.sourceSets {
            integrationTest {
                java {
                    compileClasspath += main.output + test.output
                    runtimeClasspath += main.output + test.output
                }
            }
        }

        project.dependencies {
            api group: 'de.dfki.mary', name: 'marytts-runtime', version: project.marytts.version, {
                exclude group: '*', module: 'groovy-all'
            }
            testImplementation group: 'junit', name: 'junit', version: '4.13'
            integrationTestImplementation localGroovy()
            integrationTestImplementation group: 'org.testng', name: 'testng', version: '7.0.0'
        }

        project.afterEvaluate {
            project.dependencies {
                runtimeOnly "de.dfki.mary:marytts-lang-$project.marytts.voice.language:$project.marytts.version", {
                    exclude group: '*', module: 'groovy-all'
                }
            }
        }

        project.tasks.register 'generateVoiceSource', GenerateVoiceSource, {
            dependsOn "generateSource"
            configTestFile =  project.layout.buildDirectory.file("generatedSrc/test/java/${project.marytts.component.packagePath}/ConfigTest.java")
            integrationTestFile =  project.layout.buildDirectory.file("generatedSrc/integrationTest/groovy/${project.marytts.component.packagePath}/LoadVoiceIT.groovy")

            project.sourceSets.main.java.srcDirs += "${project.buildDir}/generatedSrc/main/java"
            project.sourceSets.test.java.srcDirs += "${project.buildDir}/generatedSrc/test/java"
            project.sourceSets.integrationTest.groovy.srcDirs += "${project.buildDir}/generatedSrc/integrationTest/groovy"

            project.compileGroovy.dependsOn it
            project.compileJava.dependsOn it
            project.compileTestJava.dependsOn it
            project.compileIntegrationTestGroovy.dependsOn it
        }

        project.tasks.register 'generateVoiceConfig', GenerateVoiceConfig, {
            project.generateConfig.dependsOn it
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

        project.task('voiceIntegrationTest', type: Test) {
            useTestNG()
            workingDir = project.buildDir
            testClassesDirs = project.sourceSets.integrationTest.output.classesDirs
            classpath = project.sourceSets.integrationTest.runtimeClasspath
            systemProperty 'log4j.logger.marytts', 'INFO,stderr'
            testLogging.showStandardStreams = true
            reports.html.destination = project.file("$project.reporting.baseDir/$name")
            project.check.dependsOn it
            mustRunAfter project.test
        }

        project.task('run', type: JavaExec) {
            classpath = project.configurations.runtimeClasspath + project.sourceSets.main.output
            main = 'marytts.server.Mary'
            systemProperty 'log4j.logger.marytts', 'INFO,stderr'
        }
    }
}
