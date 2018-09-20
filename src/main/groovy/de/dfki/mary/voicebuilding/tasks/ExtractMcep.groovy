package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

class ExtractMcep extends DefaultTask {

    final WorkerExecutor workerExecutor

    @InputDirectory
    final DirectoryProperty wavDir = newInputDirectory()

    @InputDirectory
    final DirectoryProperty pmDir = newInputDirectory()

    @OutputDirectory
    final DirectoryProperty destDir = newOutputDirectory()

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
        project.fileTree(wavDir).include('*.wav').each { wavFile ->
            def basename = wavFile.name - '.wav'
            def pmFile = pmDir.file("${basename}.pm").get().asFile
            def destFile = destDir.file("${basename}.mcep").get().asFile
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
                config.isolationMode = IsolationMode.PROCESS
            }
        }
    }
}
