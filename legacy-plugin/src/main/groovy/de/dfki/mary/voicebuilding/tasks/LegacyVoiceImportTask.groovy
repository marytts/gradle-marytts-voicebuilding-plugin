package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class LegacyVoiceImportTask extends DefaultTask {

    @InputDirectory
    File srcDir

    @OutputDirectory
    File destDir

    @TaskAction
    void run() {
        project.javaexec {
            classpath project.configurations.legacy, project.configurations.compile
            main 'marytts.tools.voiceimport.DatabaseImportMain'
            workingDir project.buildDir
            args name - 'legacy'
        }
    }

}
