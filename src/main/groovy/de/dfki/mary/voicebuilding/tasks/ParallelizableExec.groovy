package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

@ParallelizableTask
class ParallelizableExec extends DefaultTask {

    @Input
    List<String> cmd

    @Optional
    @InputFiles
    List<File> srcFiles = []

    @Optional
    @OutputFile
    File destFile

    @TaskAction
    void exec() {
        project.exec {
            commandLine = cmd
        }
    }
}
