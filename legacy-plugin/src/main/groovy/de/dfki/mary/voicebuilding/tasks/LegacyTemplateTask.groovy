package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class LegacyTemplateTask extends DefaultTask {

    @OutputDirectory
    File destDir

    def resources = [
            'Config.java',
            'ConfigTest.java',
            'database.config',
            'LoadVoiceIT.java',
            'voice.config',
            'voice-hsmm.config'
    ]

    @TaskAction
    void unpack() {
        resources.each { resource ->
            def outputFile = new File(destDir, resource)
            outputFile.withOutputStream { stream ->
                stream << getClass().getResourceAsStream("/de/dfki/mary/voicebuilding/templates/$resource")
            }
        }
    }
}
