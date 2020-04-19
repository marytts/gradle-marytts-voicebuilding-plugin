package de.dfki.mary.voicebuilding.tasks

import groovy.json.JsonSlurper
import marytts.tools.voiceimport.TimelineWriter
import marytts.unitselection.data.MCepDatagram
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

class McepTimelineMaker extends TimelineMaker {

    @InputFile
    final RegularFileProperty headerFile = project.objects.fileProperty()

    @Override
    @TaskAction
    void make() {
        def props = new Properties()
        props.load(headerFile.get().asFile.newInputStream())
        def header = new ByteArrayOutputStream()
        props.store(header, '')
        def timeline = new TimelineWriter(destFile.get().asFile.path, header.toString('latin1'), sampleRate.get(), idxIntervalInSeconds.get());
        basenamesFile.get().asFile.eachLine('UTF-8') { basename ->
            def datagramFile = srcDir.file("${basename}.json").get().asFile
            def json = new JsonSlurper().parse(datagramFile)
            json.each { jsonDatagram ->
                def datagram = new MCepDatagram(jsonDatagram.duration, jsonDatagram.coeffs as float[])
                timeline.feed(datagram, sampleRate.get())
            }
        }
        timeline.close()
    }
}
