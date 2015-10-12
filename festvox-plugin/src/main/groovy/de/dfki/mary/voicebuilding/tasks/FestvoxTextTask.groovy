package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class FestvoxTextTask extends DefaultTask {
    @InputFile
    File srcFile

    @OutputDirectory
    File destDir

    @TaskAction
    void extract() {
        srcFile.eachLine { line ->
            def m = line =~ /\( (?<utt>.+) "(?<text>.+)" \)/
            if (m.matches()) {
                new File("$destDir/${m.group('utt')}.txt").text = m.group('text')
            }
        }
    }
}
