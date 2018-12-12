package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class GenerateJoinCostWeights extends DefaultTask {

    @OutputFile
    final RegularFileProperty destFile = newOutputFile()

    @TaskAction
    void generate() {
        def stream = this.getClass().getResourceAsStream('joinCostWeights.txt')
        destFile.get().asFile.withWriter { writer ->
            writer << stream
        }
    }
}
