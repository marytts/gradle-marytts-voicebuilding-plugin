package de.dfki.mary.voicebuilding

import org.gradle.api.Plugin
import org.gradle.api.Project

class VoicebuildingLegacyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.task('templates') {
            outputs.files([
                    'Config.java',
                    'ConfigTest.java',
                    'database.config',
                    'LoadVoiceIT.java',
                    'voice.config',
                    'voice-hsmm.config'
            ].collect {
                project.file "$temporaryDir/$it"
            })
            doLast {
                outputs.files.each { outputFile ->
                    outputFile.withOutputStream { stream ->
                        stream << getClass().getResourceAsStream("/de/dfki/mary/voicebuilding/templates/$outputFile.name")
                    }
                }
            }
        }
    }
}
