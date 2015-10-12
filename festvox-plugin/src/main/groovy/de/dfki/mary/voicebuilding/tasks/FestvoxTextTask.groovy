package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class FestvoxTextTask extends DefaultTask {
    @Input
    String srcFileName

    @OutputDirectory
    File destDir

    FestvoxTextTask() {
        srcFileName = 'txt.done.data'
    }

    @TaskAction
    void extract() {
        def srcFile = project.file("$project.sourceSets.data.output.resourcesDir/$srcFileName")
        srcFile.eachLine { line ->
            def m = line =~ /\( (?<utt>.+) "(?<text>.+)" \)/
            if (m.matches()) {
                new File("$destDir/${m.group('utt')}.txt").text = m.group('text')
            }
        }
    }
}
