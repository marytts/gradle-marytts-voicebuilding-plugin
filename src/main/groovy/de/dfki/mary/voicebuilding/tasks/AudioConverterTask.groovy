package de.dfki.mary.voicebuilding.tasks

import groovy.io.FileType

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class AudioConverterTask extends DefaultTask {

    @InputDirectory
    File srcDir

    @Input
    String srcFileExt = 'wav'

    @OutputDirectory
    File destDir

    @Input
    String destFileExt = 'wav'

    @TaskAction
    void process() {
        srcDir.eachFileMatch(FileType.FILES, ~/.+\.$srcFileExt/) { srcFile ->
            def destFile = project.file("$destDir/${srcFile.name.replaceAll(~/$srcFileExt$/, destFileExt)}")
            project.exec {
                executable 'sox'
                args srcFile, destFile
            }
        }
    }
}
