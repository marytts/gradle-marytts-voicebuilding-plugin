package de.dfki.mary.voicebuilding.tasks

import groovy.json.JsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

class MakeBasenameDatagrams extends DefaultTask {

    @Input
    final Property<Integer> sampleRate = project.objects.property(Integer)

    @InputFile
    final RegularFileProperty basenamesFile = project.objects.fileProperty()

    @InputDirectory
    final DirectoryProperty pmDir = project.objects.directoryProperty()

    @OutputDirectory
    final DirectoryProperty destDir = project.objects.directoryProperty()

    @TaskAction
    void make() {
        basenamesFile.get().asFile.eachLine('UTF-8') { basename ->
            def pmFile = pmDir.file("${basename}.pm").get().asFile
            def destFile = destDir.file("${basename}.json").get().asFile
            def lastTime = pmFile.readLines().last().split().first() as float
            def duration = (lastTime * sampleRate.get()) as long
            def json = new JsonBuilder([
                    [
                            duration: duration,
                            data    : basename.bytes.encodeBase64().toString()
                    ]
            ])
            destFile.text = json.toPrettyString()
        }
    }
}
