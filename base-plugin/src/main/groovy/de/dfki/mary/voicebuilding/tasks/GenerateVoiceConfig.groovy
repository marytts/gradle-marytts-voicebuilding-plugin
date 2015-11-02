package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class GenerateVoiceConfig extends DefaultTask {

    @OutputFile
    File destFile

    @TaskAction
    void generate() {
        destFile <<
                """|# Auto-generated config file for voice ${project.voice.name}
                   |
                   |name = ${project.voice.name}
                   |locale = ${project.voice.maryLocale}
                   |
                   |voice.${project.voice.name}.gender = ${project.voice.gender}
                   |voice.${project.voice.name}.locale = ${project.voice.maryLocale}
                   |voice.${project.voice.name}.domain = general
                   |voice.${project.voice.name}.samplingRate = ${project.voice.samplingRate}
                   |""".stripMargin()
    }
}
