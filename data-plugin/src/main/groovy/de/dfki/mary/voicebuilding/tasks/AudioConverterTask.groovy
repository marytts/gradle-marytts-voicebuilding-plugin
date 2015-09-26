package de.dfki.mary.voicebuilding.tasks

import groovy.io.FileType

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class AudioConverterTask extends DefaultTask {

    @InputDirectory
    File srcDir

    @OutputDirectory
    File destDir

    @TaskAction
    void process() {
        srcDir.eachFileMatch(FileType.FILES, ~/.+\.wav/) { srcFile ->
            def destFile = project.file("$destDir/$srcFile.name")
            project.exec {
                executable 'sox'
                args srcFile, destFile
            }
        }
    }
}
