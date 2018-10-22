package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

class ProcessWav extends DefaultTask {

    @Internal
    final WorkerExecutor workerExecutor

    @InputDirectory
    final DirectoryProperty srcDir = newInputDirectory()

    @OutputDirectory
    final DirectoryProperty destDir = newOutputDirectory()

    @Inject
    ProcessWav(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void process() {
        def soxPath = System.env['PATH'].split(':').collect { dir ->
            new File(dir, 'sox')
        }.find { it.exists() }
        assert soxPath
        project.fileTree(srcDir).include('**/*.wav').each { wavFile ->
            def destFile = destDir.file("$wavFile.name").get().asFile
            workerExecutor.submit(RunnableExec.class) { WorkerConfiguration config ->
                def cmd = [soxPath, wavFile, destFile, 'rate', project.marytts.voice.samplingRate]
                def args = [commandLine: cmd]
                config.params args
                config.isolationMode = IsolationMode.PROCESS
            }
        }
    }
}
