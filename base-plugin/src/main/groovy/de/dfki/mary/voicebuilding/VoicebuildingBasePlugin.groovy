package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.GenerateSource

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

        project.task('generateSource', type: GenerateSource) {
            destDir = project.file("$project.buildDir/generatedSrc")
            project.compileJava.dependsOn it
            project.compileTestJava.dependsOn it
        }
    }
}
