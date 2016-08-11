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
        def processed = 0
        srcFile.eachLine { line ->
            def m = line =~ /\( (?<utt>.+) "(?<text>.+)" \)/
            if (m.matches()) {
                def destFile = new File("$destDir/${m.group('utt')}.txt")
                project.logger.debug "Wrote $destFile"
                destFile.text = m.group('text')
                processed += 1
            } else {
                project.logger.warn "Could not process line:\n$line"
            }
        }
        project.logger.info "Extracted $processed utterances into $destDir"
    }
}
