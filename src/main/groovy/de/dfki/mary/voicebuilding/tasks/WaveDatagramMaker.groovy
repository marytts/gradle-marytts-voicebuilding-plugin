package de.dfki.mary.voicebuilding.tasks

import groovy.json.JsonBuilder
import marytts.tools.voiceimport.WavReader
import marytts.util.data.ESTTrackReader
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

interface WaveDatagramMakerParameters extends WorkParameters {

    RegularFileProperty getWavFile()

    RegularFileProperty getPmFile()

    RegularFileProperty getDestFile()

    Property<Integer> getSampleRate()
}

abstract class WaveDatagramMaker implements WorkAction<WaveDatagramMakerParameters> {

    @Override
    void execute() {
        def pm = new ESTTrackReader(parameters.pmFile.get().asFile.path)
        def wav = new WavReader(parameters.wavFile.get().asFile.path)
        def wave = wav.samples
        def jsonDatagrams = []
        int frameStart
        int frameEnd = 0
        pm.times.each { time ->
            frameStart = frameEnd
            frameEnd = (time * parameters.sampleRate.get()) as int
            def duration = frameEnd - frameStart
            def baos = new ByteArrayOutputStream(2 * duration)
            def dos = new DataOutputStream(baos)
            if (duration > 0) {
                (frameStart..frameEnd - 1).each { f ->
                    dos.writeShort wave[f]
                }
            }
            jsonDatagrams << [
                    duration: duration,
                    data    : baos.toByteArray().encodeBase64().toString()
            ]
        }
        def json = new JsonBuilder(jsonDatagrams)
        parameters.destFile.get().asFile.text = json.toPrettyString()
    }
}
