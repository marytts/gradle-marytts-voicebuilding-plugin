package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.*

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class VoicebuildingBasePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply JavaPlugin

        project.extensions.add 'voice', VoiceExtension

        project.ext {
            maryttsVersion = '5.1.1'
        }

        project.repositories {
            jcenter()
        }

        project.dependencies {
            compile(group: 'de.dfki.mary', name: 'marytts-runtime', version: project.maryttsVersion) {
                exclude module: 'freetts'
                exclude module: 'freetts-en_us'
                exclude module: 'freetts-de'
            }
            testCompile group: 'junit', name: 'junit', version: '4.12'
        }

        project.task('generateSource', type: GenerateSource) {
            destDir = project.file("$project.buildDir/generatedSrc")
            project.sourceSets.main.java.srcDirs += "$destDir/main/java"
            project.sourceSets.test.java.srcDirs += "$destDir/test/java"
            project.compileJava.dependsOn it
            project.compileTestJava.dependsOn it
        }

        project.task('generateVoiceConfig', type: GenerateVoiceConfig) {
            project.afterEvaluate {
                destFile = project.file("$project.sourceSets.main.output.resourcesDir/marytts/voice/$project.voice.nameCamelCase/voice.config")
            }
            project.processResources.dependsOn it
        }
    }
}
