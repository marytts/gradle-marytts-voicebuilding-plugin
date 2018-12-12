package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

class MakeMcepDatagrams extends DefaultTask {

    @Internal
    final WorkerExecutor workerExecutor

    @Input
    Property<Integer> sampleRate = project.objects.property(Integer)

    @InputFile
    final RegularFileProperty basenamesFile = newInputFile()

    @InputDirectory
    final DirectoryProperty mcepDir = newInputDirectory()

    @OutputDirectory
    final DirectoryProperty destDir = newOutputDirectory()

    @Inject
    MakeMcepDatagrams(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void make() {
        basenamesFile.get().asFile.eachLine('UTF-8') { basename ->
            def mcepFile = mcepDir.file("${basename}.mcep").get().asFile
            def destFile = destDir.file("${basename}.json").get().asFile
            workerExecutor.submit(McepDatagramMaker.class) { config ->
                config.params mcepFile, destFile, sampleRate.get()
                config.isolationMode = IsolationMode.PROCESS
            }
        }
    }
}
