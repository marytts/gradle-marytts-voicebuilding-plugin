package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class TextTask extends DefaultTask {

    @Input
    String dataFileName = 'time.data'

    @Input
    File dataFile

    @OutputDirectory
    File textDir

    @TaskAction
    void extract() {
        dataFile.eachLine { line ->
            def m = line =~ /\( (?<utt>.+) "(?<text>.+)" \)/
            if (m.matches()) {
                new File("$textDir/${m.group('utt')}.txt").text = m.group('text')
            }
        }
    }
}
