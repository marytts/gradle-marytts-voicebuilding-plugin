package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

class MakeWaveDatagrams extends DefaultTask {

    @Internal
    final WorkerExecutor workerExecutor

    @Input
    final Property<Integer> sampleRate = project.objects.property(Integer)

    @InputFile
    final RegularFileProperty basenamesFile = project.objects.fileProperty()

    @InputDirectory
    final DirectoryProperty wavDir = project.objects.directoryProperty()

    @InputDirectory
    final DirectoryProperty pmDir = project.objects.directoryProperty()

    @OutputDirectory
    final DirectoryProperty destDir = project.objects.directoryProperty()

    @Inject
    MakeWaveDatagrams(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void make() {
        basenamesFile.get().asFile.eachLine('UTF-8') { basename ->
            def wavFile = wavDir.file("${basename}.wav").get().asFile
            def pmFile = pmDir.file("${basename}.pm").get().asFile
            def destFile = destDir.file("${basename}.json").get().asFile
            workerExecutor.submit(WaveDatagramMaker.class) { config ->
                config.params wavFile, pmFile, destFile, sampleRate.get()
                config.isolationMode = IsolationMode.PROCESS
            }
        }
    }
}
