package de.dfki.mary.voicebuilding.tasks

import com.google.gson.Gson
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
        // TODO: workaround for JSON serialization in Gradle v5.0
        // java.util.ServiceConfigurationError:
        // org.apache.groovy.json.FastStringServiceFactory:
        // Provider org.apache.groovy.json.DefaultFastStringServiceFactory not a subtype
        def json = new Gson().toJson(jsonDatagrams)
        destFile.text = json
    }
}
