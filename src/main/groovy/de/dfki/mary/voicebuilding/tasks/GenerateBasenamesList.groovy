package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class GenerateBasenamesList extends DefaultTask {

    @InputDirectory
    final DirectoryProperty wavDir = newInputDirectory()

    @OutputFile
    final RegularFileProperty destFile = newOutputFile()

    @TaskAction
    void generate() {
        destFile.get().asFile.withWriter('UTF-8') { writer ->
            project.fileTree(wavDir).include('*.wav').each { wavFile ->
                def basename = wavFile.name - '.wav'
                writer.println basename
            }
        }
    }
}
