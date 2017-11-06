package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class PraatExec extends DefaultTask {

    @Input
    String command = 'praat'

    @InputFile
    File scriptFile

    @Optional
    @Input
    Map props = [:]

    @Optional
    @InputFiles
    List<File> srcFiles = []

    @Optional
    @OutputFile
    File destFile

    @TaskAction
    void exec() {
        project.copy {
            from scriptFile
            into temporaryDir
            expand props
        }
        project.exec {
            def legacyPraat = "${System.env['LEGACY_PRAAT']}".toBoolean()
            project.logger.debug "legacyPraat == $legacyPraat"
            if (legacyPraat) {
                commandLine command, scriptFile.name
            } else {
                commandLine command, '--run', scriptFile.name
            }
            workingDir temporaryDir
        }
    }
}
