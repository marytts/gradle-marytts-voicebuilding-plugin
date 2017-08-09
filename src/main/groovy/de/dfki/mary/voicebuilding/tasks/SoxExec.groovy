package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

@ParallelizableTask
class SoxExec extends DefaultTask {

    @Input
    String command = 'sox'

    @Input
    List<String> args = []

    @InputFile
    File srcFile

    @OutputFile
    File destFile

    @TaskAction
    void exec() {
        project.exec {
            commandLine command, srcFile, destFile
            if (args) {
                commandLine << args
            }
        }
    }
}
