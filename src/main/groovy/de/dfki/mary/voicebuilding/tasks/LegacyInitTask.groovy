package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class LegacyInitTask extends DefaultTask {

    @OutputFile
    File configFile = project.file("$project.buildDir/database.config")

    @TaskAction
    void init() {
        project.copy {
            from project.templates
            into project.buildDir
            include configFile.name
            expand project.properties
        }
    }
}
