package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class GenerateVoiceConfig extends DefaultTask {

    @Input
    final MapProperty<String, Object> config = project.objects.mapProperty(String, Object)

    GenerateVoiceConfig() {
        this.config.set([:])
    }

    @TaskAction
    void generate() {

        def engine = new groovy.text.SimpleTemplateEngine()
        def bindings = ["project" : project]
        def tmp_config = [:]
        tmp_config["name"] = project.marytts.voice.name
        tmp_config["locale"] = project.marytts.voice.maryLocale
        tmp_config["${voiceType()}.voices.list"] = project.marytts.voice.name

        ([
            domain      : 'general',
            gender      : project.marytts.voice.gender,
            locale      : project.marytts.voice.locale,
            samplingRate: project.marytts.voice.samplingRate
        ] + config.get()).each { key, value ->
            tmp_config["voice.${project.marytts.voice.name}.$key"] = engine.createTemplate(value.toString()).make(bindings)
        }

        project.marytts.component.config = tmp_config

    }

    String voiceType() {
        def type
        switch (project.marytts.voice.type) {
            case ~/hs?mm/:
                type = 'hmm'
                break
            default:
                type = 'unitselection'
        }
        return type
    }
}
