package de.dfki.mary.voicebuilding.tasks

import groovy.json.JsonBuilder
import marytts.util.data.ESTTrackReader

import javax.inject.Inject

class McepDatagramMaker implements Runnable {

    File mcepFile

    File destFile

    int sampleRate

    @Inject
    McepDatagramMaker(File mcepFile, File destFile, int sampleRate) {
        this.mcepFile = mcepFile
        this.destFile = destFile
        this.sampleRate = sampleRate
    }

    @Override
    void run() {
        def mcep = new ESTTrackReader(mcepFile.path)
        def jsonDatagrams = []
        int frameStart
        int frameEnd = 0
        mcep.frames.eachWithIndex { frame, f ->
            frameStart = frameEnd
            def time = mcep.getTime(f)
            frameEnd = (time * sampleRate) as int
            def duration = frameEnd - frameStart
            jsonDatagrams << [
                    duration: duration,
                    coeffs  : frame
            ]
        }
        def json = new JsonBuilder(jsonDatagrams)
        destFile.text = json.toPrettyString()
    }
}
