package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.*

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.testing.Test

import org.unbrokendome.gradle.plugins.testsets.TestSetsPlugin

class VoicebuildingBasePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply GroovyPlugin
        project.plugins.apply MavenPlugin
        project.plugins.apply TestSetsPlugin

        project.sourceCompatibility = '1.7'

        project.extensions.create 'voice', VoiceExtension
        project.voice.extensions.create 'license', VoiceLicenseExtension

        project.ext {
            maryttsVersion = '5.1.1'
        }

        project.repositories {
            jcenter()
        }

        project.testSets {
            integrationTest
        }

        project.dependencies {
            compile(group: 'de.dfki.mary', name: 'marytts-runtime', version: project.maryttsVersion) {
                exclude module: 'freetts'
                exclude module: 'freetts-en_us'
                exclude module: 'freetts-de'
            }
            testCompile group: 'junit', name: 'junit', version: '4.12'
            integrationTestCompile localGroovy()
            integrationTestCompile group: 'org.testng', name: 'testng', version: '6.9.4'
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
                destFile = project.file("${project.sourceSets.main.output.resourcesDir}/META-INF/maven/${project.group.replace('.', '/')}/voice-$project.voice.name/pom.xml")
            }
            project.jar.dependsOn it
        }

        project.task('generatePomProperties', type: GeneratePomProperties) {
            project.afterEvaluate {
                destFile = project.file("${project.sourceSets.main.output.resourcesDir}/META-INF/maven/${project.group.replace '.', '/'}/voice-$project.voice.name/pom.properties")
            }
            project.jar.dependsOn it
        }

        project.integrationTest {
            useTestNG()
            systemProperty 'log4j.logger.marytts', 'INFO,stderr'
            testLogging.showStandardStreams = true
            reports.html.destination = project.file("$project.reporting.baseDir/$name")
            project.check.dependsOn it
            mustRunAfter project.test
        }
    }
}
