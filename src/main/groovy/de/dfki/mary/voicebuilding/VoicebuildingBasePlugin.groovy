package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.*

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test

class VoicebuildingBasePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply GroovyPlugin
        project.plugins.apply MavenPlugin

        project.sourceCompatibility = '1.7'

        project.extensions.create 'voice', VoiceExtension
        project.voice.extensions.create 'license', VoiceLicenseExtension

        project.ext {
            maryttsVersion = this.getClass().getResource('/maryttsVersion.txt')?.text
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

        project.configurations {
            integrationTestCompile.extendsFrom testCompile
            integrationTestRuntime.extendsFrom testRuntime
        }

        project.dependencies {
            compile group: 'de.dfki.mary', name: 'marytts-runtime', version: project.maryttsVersion, {
                exclude group: '*', module: 'groovy-all'
            }
            testCompile group: 'junit', name: 'junit', version: '4.12'
            integrationTestCompile localGroovy()
            integrationTestCompile group: 'org.testng', name: 'testng', version: '6.14.3'
        }

        project.afterEvaluate {
            project.dependencies {
                runtime "de.dfki.mary:marytts-lang-$project.voice.language:$project.maryttsVersion", {
                    exclude group: '*', module: 'groovy-all'
                }
            }
        }

        project.task('generateSource', type: GenerateSource) {
            destDir = project.file("$project.buildDir/generatedSrc")
            project.sourceSets.main.java.srcDirs += "$destDir/main/java"
            project.sourceSets.test.java.srcDirs += "$destDir/test/java"
            project.sourceSets.integrationTest.groovy.srcDirs += "$destDir/integrationTest/groovy"
            project.compileJava.dependsOn it
            project.compileTestJava.dependsOn it
            project.compileIntegrationTestGroovy.dependsOn it
            doFirst {
                assert destDir.path.startsWith(project.buildDir.path)
                project.delete destDir
            }
        }

        project.task('generateVoiceConfig', type: GenerateVoiceConfig) {
            project.afterEvaluate {
                destFile = project.file("$project.sourceSets.main.output.resourcesDir/marytts/voice/$project.voice.nameCamelCase/voice.config")
            }
            project.processResources.dependsOn it
        }

        project.task('generateServiceLoader', type: GenerateServiceLoader) {
            destFile = project.file("$project.sourceSets.main.output.resourcesDir/META-INF/services/marytts.config.MaryConfig")
            project.processResources.dependsOn it
        }

        project.task('generatePom', type: GeneratePom) {
            project.afterEvaluate {
                destFile = project.file("${project.sourceSets.main.output.resourcesDir}/META-INF/maven/$project.group/voice-$project.voice.name/pom.xml")
            }
            project.jar.dependsOn it
        }

        project.task('generatePomProperties', type: GeneratePomProperties) {
            project.afterEvaluate {
                destFile = project.file("${project.sourceSets.main.output.resourcesDir}/META-INF/maven/$project.group/voice-$project.voice.name/pom.properties")
            }
            project.jar.dependsOn it
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
