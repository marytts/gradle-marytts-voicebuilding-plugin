package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class GenerateVoiceConfig extends DefaultTask {

    @Input
    Map config = [
            domain      : 'general',
            gender      : project.voice.gender,
            locale      : project.voice.locale,
            samplingRate: project.voice.samplingRate
    ]

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
                   |""".stripMargin()
        destFile << config.collect { key, value ->
            "voice.${project.voice.name}.$key = $value"
        }.join('\n')
    }
}
