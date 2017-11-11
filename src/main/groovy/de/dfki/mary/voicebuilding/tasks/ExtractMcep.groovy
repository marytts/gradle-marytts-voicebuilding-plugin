package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.workers.*

import javax.inject.Inject

class ExtractMcep extends DefaultTask {

    @Internal
    final WorkerExecutor workerExecutor

    @InputFiles
    FileCollection wavFiles

    @InputFiles
    FileCollection pmFiles

    @OutputDirectory
    File destDir

    @Inject
    ExtractMcep(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void process() {
        def sig2FvPath = System.env['PATH'].split(':').collect { dir ->
            new File(dir, 'sig2fv')
        }.find { it.exists() }
        assert sig2FvPath
        wavFiles.each { wavFile ->
            def basename = wavFile.name - '.wav'
            def pmFile = project.file("$project.pitchmarkConverter.destDir/${basename}.pm")
            def destFile = project.file("$destDir/${basename}.mcep")
            workerExecutor.submit(RunnableExec.class) { WorkerConfiguration config ->
                def cmd = [sig2FvPath,
                           '-window_type', 'hamming',
                           '-factor', 2.5,
                           '-otype', 'est_binary',
                           '-coefs', 'melcep',
                           '-melcep_order', 12,
                           '-fbank_order', 24,
                           '-shift', 0.01,
                           '-preemph', 0.97,
                           '-pm', pmFile,
                           '-o', destFile,
                           wavFile]
                def args = [commandLine: cmd]
                config.params args
            }
        }
    }
}
