package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class PitchmarkConverter extends DefaultTask {

    @InputFile
    File srcFile

    @OutputFile
    File destFile

    @TaskAction
    void convert() {
        def nx
        def times = []
        srcFile.readLines().eachWithIndex { line, l ->
            switch (l) {
                case { it < 5 }:
                    // ignore praat header...
                    break
                case 5:
                    // ...up to line 5, which tells number of points
                    nx = line
                    break
                default:
                    times << line
            }
        }
        destFile.withWriter { pm ->
            pm.println 'EST_File Track'
            pm.println 'DataType ascii'
            pm.println 'NumFrames ' + nx
            pm.println 'NumChannels 0'
            pm.println 'NumAuxChannels 0'
            pm.println 'EqualSpace 0'
            pm.println 'BreaksPresent true'
            pm.println 'CommentChar ;'
            pm.println 'EST_Header_End'
            pm.println times.collect { "${it as float}\t1" }.join('\n')
        }
    }
}
