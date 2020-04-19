package de.dfki.mary.voicebuilding.tasks

import marytts.util.data.ESTTrackReader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class GenerateMcepTimelineHeader extends DefaultTask {

    @InputDirectory
    final DirectoryProperty srcDir = project.objects.directoryProperty()

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    @TaskAction
    void generate() {
        // get order, range of values from EST MCEP files
        def mcepOrder
        def mcepMin
        def mcepMax
        project.fileTree(srcDir).include('*.mcep').each { mcepFile ->
            def mcep = new ESTTrackReader(mcepFile.path)
            mcepOrder = mcep.numChannels
            def minMax = mcep.minMax
            if (!mcepMin || mcepMin > minMax[0]) {
                mcepMin = minMax[0]
            }
            if (!mcepMax || mcepMax < minMax[1]) {
                mcepMax = minMax[1]
            }
        }
        assert mcepOrder
        def mcepRange = (mcepMax - mcepMin) as float
        // init header
        def cmd = '\n$ESTDIR/bin/sig2fv -window_type hamming -factor 2.5 -otype est_binary -coefs melcep -melcep_order 12' +
                ' -fbank_order 24 -shift 0.01 -preemph 0.97 -pm PITCHMARKFILE.pm -o melcepDir/mcepFile.mcep WAVDIR/WAVFILE.wav\n'
        def props = new Properties()
        props << [
                command     : cmd,
                'mcep.order': mcepOrder as String,
                'mcep.min'  : mcepMin as String,
                'mcep.range': mcepRange as String
        ]
        props.store(destFile.get().asFile.newOutputStream(), '')
    }
}
