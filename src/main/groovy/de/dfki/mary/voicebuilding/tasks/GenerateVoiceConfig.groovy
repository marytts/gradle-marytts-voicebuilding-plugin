package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class GenerateVoiceConfig extends DefaultTask {

    @Input
    Map config = [:]

    @OutputFile
    File destFile

    @Internal
    String voiceType

    @TaskAction
    void generate() {
        config << [
                domain      : 'general',
                gender      : project.marytts.voice.gender,
                locale      : project.marytts.voice.locale,
                samplingRate: project.marytts.voice.samplingRate
        ]
        destFile <<
                """|# Auto-generated config file for voice ${project.marytts.voice.name}
                   |
                   |name = ${project.marytts.voice.name}
                   |locale = ${project.marytts.voice.maryLocale}
                   |
                   |${getVoiceType()}.voices.list = ${project.marytts.voice.name}
                   |
                   |""".stripMargin()
        destFile << config.collect { key, value ->
            "voice.${project.marytts.voice.name}.$key = $value"
        }.join('\n')
    }

    String getVoiceType() {
        def type
        switch (project.marytts.voice.type) {
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
