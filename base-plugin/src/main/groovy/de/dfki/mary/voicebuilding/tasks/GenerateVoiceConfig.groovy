package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class GenerateVoiceConfig extends DefaultTask {

    Map config = [:]

    @OutputFile
    File destFile

    @TaskAction
    void generate() {
        config << [
                domain      : 'general',
                gender      : project.voice.gender,
                locale      : project.voice.locale,
                samplingRate: project.voice.samplingRate
        ]
        destFile <<
                """|# Auto-generated config file for voice ${project.voice.name}
                   |
                   |name = ${project.voice.name}
                   |locale = ${project.voice.maryLocale}
                   |
                   |${voiceType}.voices.list = ${project.voice.name}
                   |
                   |""".stripMargin()
        destFile << config.collect { key, value ->
            "voice.${project.voice.name}.$key = $value"
        }.join('\n')
    }

    String getVoiceType() {
        def type
        switch (project.voice.type) {
            case ~/hs?mm/:
                type = 'hmm'
                break
            default:
                type = 'unitselection'
                break
        }
        type
    }
}
