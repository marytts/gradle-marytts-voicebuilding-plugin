package de.dfki.mary.voicebuilding.tasks

import groovy.json.JsonBuilder
import marytts.util.data.ESTTrackReader
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

interface McepDatagramMakerParameters extends WorkParameters {

    RegularFileProperty getMcepFile()

    RegularFileProperty getDestFile()

    Property<Integer> getSampleRate()
}

abstract class McepDatagramMaker implements WorkAction<McepDatagramMakerParameters> {

    @Override
    void execute() {
        def mcep = new ESTTrackReader(parameters.mcepFile.get().asFile.path)
        def jsonDatagrams = []
        int frameStart
        int frameEnd = 0
        mcep.frames.eachWithIndex { frame, f ->
            frameStart = frameEnd
            def time = mcep.getTime(f)
            frameEnd = (time * parameters.sampleRate.get()) as int
            def duration = frameEnd - frameStart
            jsonDatagrams << [
                    duration: duration,
                    coeffs  : frame
            ]
        }
        def json = new JsonBuilder(jsonDatagrams)
        parameters.destFile.get().asFile.text = json.toPrettyString()
    }
}
