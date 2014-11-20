package de.dfki.mary.voicebuilding.tasks.legacy

import org.gradle.api.tasks.JavaExec

class LegacyVoiceImportTask extends JavaExec {

    LegacyVoiceImportTask() {
        classpath project.configurations.legacy, project.configurations.compile
        main 'marytts.tools.voiceimport.DatabaseImportMain'
        systemProperties = ['user.dir': project.buildDir]
        args name.replace('legacy', '')
    }

}
