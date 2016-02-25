package de.dfki.mary.voicebuilding.tasks

import groovy.io.FileType

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class PraatPitchmarkerTask extends DefaultTask {

    @InputDirectory
    File srcDir

    @OutputDirectory
    File destDir

    @Input
    String gender = 'female'

    @Input
    int minPitch = (gender == 'female') ? 100 : 75

    @Input
    int maxPitch = (gender == 'female') ? 500 : 300

    @TaskAction
    void process() {
        // generate praat script
        def scriptFile = project.file("$temporaryDir/script.praat")
        def resource = getClass().getResourceAsStream('script.praat')
        assert resource
        scriptFile.withOutputStream { stream ->
            stream << resource
        }

        // generate input file list
        def basenames = srcDir.listFiles().findAll { it.name.endsWith('.wav') }.collect { it.name - '.wav' }
        def basenamesFile = project.file("$temporaryDir/basenames.lst")
        basenamesFile.text = basenames.join('\n')

        // test for Praat 6
        def praat = 'praat'
        try {
            'praat --version'.execute()
            praat = 'praat --run'
        } catch (all) {
            // assume we have Praat 5
        }

        // run praat script
        project.exec {
            commandLine = praat.tokenize() + [scriptFile, basenamesFile, srcDir, destDir, minPitch, maxPitch]
        }

        // convert praat .PointProcess file to EST .pm file
        destDir.eachFileMatch(FileType.FILES, ~/.+\.PointProcess/) { ppFile ->
            def pmFile = project.file("$destDir/${ppFile.name.replace('.PointProcess', '.pm')}")
            def nx
            def times = []
            ppFile.readLines().eachWithIndex { line, l ->
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
            pmFile.withWriter { pm ->
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
}
