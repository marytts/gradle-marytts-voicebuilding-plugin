package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class GeneratePomProperties extends DefaultTask {

    @OutputFile
    File destFile

    @TaskAction
    void generate() {
        destFile <<
                """|version=$project.version
                   |groupId=$project.group
                   |artifactId=$project.name
                   |""".stripMargin()
    }
}
