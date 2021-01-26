package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

class ProcessWav extends DefaultTask {

    @Internal
    final WorkerExecutor workerExecutor

    @InputDirectory
    final DirectoryProperty srcDir = project.objects.directoryProperty()

    @OutputDirectory
    final DirectoryProperty destDir = project.objects.directoryProperty()

    @Inject
    ProcessWav(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void process() {
        def binaryName = 'sox'
        def pathEnv = System.env.PATH
        if (OperatingSystem.current().isWindows()) {
            binaryName += '.exe'
            pathEnv = System.env.Path
        }
        def binaryPath = pathEnv.split(File.pathSeparator).collect { dir ->
            new File(dir, binaryName)
        }.find { it.canExecute() }
        assert binaryPath: "Could not find PATH to $binaryName"
        def workQueue = workerExecutor.processIsolation()
        project.fileTree(srcDir).include('**/*.wav').each { wavFile ->
            def destFile = destDir.file("$wavFile.name").get().asFile
            workQueue.submit(RunnableExec.class) { parameters ->
                parameters.commandLine = [
                        binaryPath,
                        wavFile,
                        destFile,
                        'rate', project.marytts.voice.samplingRate
                ].collect { it.toString() }
            }
        }
    }
}
