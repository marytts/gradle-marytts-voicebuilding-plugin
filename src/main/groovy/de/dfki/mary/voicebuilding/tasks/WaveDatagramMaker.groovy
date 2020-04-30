package de.dfki.mary.voicebuilding.tasks

import groovy.json.JsonBuilder
import marytts.tools.voiceimport.WavReader
import marytts.util.data.ESTTrackReader

import javax.inject.Inject

class WaveDatagramMaker implements Runnable {

    File wavFile

    File pmFile

    File destFile

    int sampleRate

    @Inject
    WaveDatagramMaker(File wavFile, File pmFile, File destFile, int sampleRate) {
        this.wavFile = wavFile
        this.pmFile = pmFile
        this.destFile = destFile
        this.sampleRate = sampleRate
    }

    @Override
    void run() {
        def pm = new ESTTrackReader(pmFile.path)
        def wav = new WavReader(wavFile.path)
        def wave = wav.samples
        def jsonDatagrams = []
        int frameStart
        int frameEnd = 0
        pm.times.each { time ->
            frameStart = frameEnd
            frameEnd = (time * sampleRate) as int
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
        destFile.text = json.toPrettyString()
    }
}
