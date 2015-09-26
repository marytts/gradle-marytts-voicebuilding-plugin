package de.dfki.mary.voicebuilding.tasks

import groovy.io.FileType

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class MCEPMakerTask extends DefaultTask {

    @InputDirectory
    File srcDir

    @InputDirectory
    File pmDir

    @OutputDirectory
    File destDir

    @TaskAction
    void process() {
        srcDir.eachFileMatch(FileType.FILES, ~/.+\.wav/) { wavFile ->
            def pmFile = project.file("$pmDir/${wavFile.name.replace('.wav', '.pm')}")
            def mcepFile = project.file("$destDir/${wavFile.name.replace('.wav', '.mcep')}")
            project.exec {
                executable 'sig2fv'
                args '-window_type', 'hamming',
                        '-factor', 2.5,
                        '-otype', 'est_binary',
                        '-coefs', 'melcep',
                        '-melcep_order', 12,
                        '-fbank_order', 24,
                        '-shift', 0.01,
                        '-preemph', 0.97,
                        '-pm', pmFile,
                        '-o', mcepFile,
                        wavFile
            }
        }
    }
}
