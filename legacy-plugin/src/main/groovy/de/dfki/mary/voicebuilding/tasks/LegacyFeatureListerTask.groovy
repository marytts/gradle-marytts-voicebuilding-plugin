package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class LegacyFeatureListerTask extends DefaultTask {

    @OutputFile
    File destFile

    @TaskAction
    void generate() {
        project.javaexec {
            classpath project.sourceSets.marytts.runtimeClasspath
            main 'marytts.FeatureLister'
            systemProperties = [
                    locale    : project.voice.maryLocale,
                    outputFile: destFile
            ]
        }
    }
}
