package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

class MakeMcepDatagrams extends DefaultTask {

    @Internal
    final WorkerExecutor workerExecutor

    @Input
    final Property<Integer> sampleRate = project.objects.property(Integer)

    @InputFile
    final RegularFileProperty basenamesFile = project.objects.fileProperty()

    @InputDirectory
    final DirectoryProperty mcepDir = project.objects.directoryProperty()

    @OutputDirectory
    final DirectoryProperty destDir = project.objects.directoryProperty()

    @Inject
    MakeMcepDatagrams(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void make() {
        def workQueue = workerExecutor.processIsolation()
        basenamesFile.get().asFile.eachLine('UTF-8') { basename ->
            workQueue.submit(McepDatagramMaker.class) { parameters ->
                parameters.mcepFile = mcepDir.file("${basename}.mcep")
                parameters.destFile = destDir.file("${basename}.json")
                parameters.sampleRate = this.sampleRate
            }
        }
    }
}
