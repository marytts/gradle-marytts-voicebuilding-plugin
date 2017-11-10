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
    List args = []

    @Optional
    @InputFiles
    List<File> srcFiles = []

    @Optional
    @OutputFile
    File destFile

    @TaskAction
    void exec() {
        project.exec {
            def legacyPraat = "${System.env['LEGACY_PRAAT']}".toBoolean()
            project.logger.debug "legacyPraat == $legacyPraat"
            if (legacyPraat) {
                commandLine command, scriptFile
            } else {
                commandLine command, '--run', scriptFile
            }
            commandLine += this.args
        }
    }
}
