package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

class GenerateServiceLoader extends DefaultTask {

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    @TaskAction
    void generate() {
        destFile.get().asFile.text = "marytts.voice.${project.marytts.voice.nameCamelCase}.Config"
    }
}
