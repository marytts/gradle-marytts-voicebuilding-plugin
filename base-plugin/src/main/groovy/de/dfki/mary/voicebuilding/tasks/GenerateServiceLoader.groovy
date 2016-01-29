package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class GenerateServiceLoader extends DefaultTask {

    @OutputFile
    File destFile

    @TaskAction
    void generate() {
        destFile.text = "marytts.voice.${project.voice.nameCamelCase}.Config"
    }
}
