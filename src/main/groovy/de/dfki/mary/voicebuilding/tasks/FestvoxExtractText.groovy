package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class FestvoxExtractText extends DefaultTask {

    @InputFile
    final RegularFileProperty srcFile = newInputFile()

    @OutputDirectory
    final DirectoryProperty destDir = newOutputDirectory()

    @TaskAction
    void extract() {
        def processed = 0
        srcFile.get().asFile.eachLine { line ->
            def m = line =~ /\( ?(?<utt>.+) "(?<text>.+)" ?\)/
            if (m.matches()) {
                def destFile = destDir.file("${m.group('utt')}.txt").get().asFile
                project.logger.debug "Wrote $destFile"
                destFile.text = m.group('text').trim()
                processed += 1
            } else {
                project.logger.warn "Could not process line:\n$line"
            }
        }
        if (processed < 1) {
            throw new GradleException("Could not extract any utterances from $srcFile")
        }
        project.logger.info "Extracted $processed utterances into ${destDir.get().asFile}"
    }
}
