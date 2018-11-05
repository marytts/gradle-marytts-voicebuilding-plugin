package de.dfki.mary.voicebuilding

import de.dfki.mary.MaryttsExtension
import de.dfki.mary.voicebuilding.tasks.*

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.WriteProperties
import org.gradle.api.tasks.testing.Test

class VoicebuildingBasePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply GroovyPlugin
        project.plugins.apply MavenPublishPlugin

        project.sourceCompatibility = '1.8'

        project.extensions.create 'marytts', MaryttsExtension, project
        project.marytts {
            version = this.getClass().getResource('/maryttsVersion.txt')?.text
        }

        project.marytts.extensions.create 'voice', VoiceExtension, project
        project.marytts.voice.extensions.create 'license', VoiceLicenseExtension, project

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

        project.configurations {
            integrationTestCompile.extendsFrom testCompile
            integrationTestRuntime.extendsFrom testRuntime
        }

        project.dependencies {
            compile group: 'de.dfki.mary', name: 'marytts-runtime', version: project.marytts.version, {
                exclude group: '*', module: 'groovy-all'
            }
            testCompile group: 'junit', name: 'junit', version: '4.12'
            integrationTestCompile localGroovy()
            integrationTestCompile group: 'org.testng', name: 'testng', version: '6.14.3'
        }

        project.afterEvaluate {
            project.dependencies {
                runtimeOnly "de.dfki.mary:marytts-lang-$project.marytts.voice.language:$project.marytts.version", {
                    exclude group: '*', module: 'groovy-all'
                }
            }
        }

        project.task('generateSource', type: GenerateSource) {
            destDir = project.layout.buildDirectory.dir('generatedSrc')
            project.sourceSets.main.java.srcDirs += "${destDir.get().asFile}/main/java"
            project.sourceSets.test.java.srcDirs += "${destDir.get().asFile}/test/java"
            project.sourceSets.integrationTest.groovy.srcDirs += "${destDir.get().asFile}/integrationTest/groovy"
            project.compileJava.dependsOn it
            project.compileTestJava.dependsOn it
            project.compileIntegrationTestGroovy.dependsOn it
        }

        project.task('generateVoiceConfig', type: GenerateVoiceConfig) {
            destFile = project.layout.buildDirectory.file('voice.config')
        }

        project.task('generateServiceLoader', type: GenerateServiceLoader) {
            destFile = project.layout.buildDirectory.file('serviceLoader.txt')
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
            from project.generateVoiceConfig, {
                rename { "marytts/voice/$project.marytts.voice.nameCamelCase/voice.config" }
            }
            from project.generateServiceLoader, {
                rename { 'META-INF/services/marytts.config.MaryConfig' }
            }
            from project.generatePomProperties, {
                rename { "META-INF/maven/$project.group/voice-$project.marytts.voice.name/pom.xml" }
            }
            from project.generatePomProperties, {
                rename { "META-INF/maven/$project.group/voice-$project.marytts.voice.name/pom.properties" }
            }
        }

        project.task('integrationTest', type: Test) {
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
            classpath = project.configurations.runtime + project.sourceSets.main.output
            main = 'marytts.server.Mary'
            systemProperty 'log4j.logger.marytts', 'INFO,stderr'
        }
    }
}
