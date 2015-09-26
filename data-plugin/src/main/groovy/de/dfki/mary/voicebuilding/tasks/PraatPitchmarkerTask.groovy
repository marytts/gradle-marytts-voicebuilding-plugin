package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

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
        def resource = getClass().getResourceAsStream('/marytts/tools/voiceimport/script.praat')
        scriptFile.withOutputStream { stream ->
            stream << resource
        }

        // generate input file list
        def basenames = srcDir.listFiles().findAll { it.name.endsWith('.wav') }.collect { it.name - '.wav' }
        def basenamesFile = project.file("$temporaryDir/basenames.lst")
        basenamesFile.text = basenames.join('\n')

        // run praat script
        project.exec {
            executable 'praat'
            args scriptFile, basenamesFile, srcDir, destDir, minPitch, maxPitch
        }

        // TODO: convert to EST .pm files
    }
}
