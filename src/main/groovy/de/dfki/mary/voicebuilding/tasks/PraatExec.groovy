package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class PraatExec extends DefaultTask {

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
            commandLine project.praat.binary, scriptFile.name
            workingDir temporaryDir
        }
    }
}
