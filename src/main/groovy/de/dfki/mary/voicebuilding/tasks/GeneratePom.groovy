package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class GeneratePom extends DefaultTask {

    @OutputFile
    File destFile

    @TaskAction
    void generate() {
        project.pom { pom ->
            pom.project {
                description project.marytts.voice.description
                licenses {
                    license {
                        name project.marytts.voice.license.name
                        url project.marytts.voice.license.url
                    }
                }
            }
        }.writeTo(destFile)
    }
}
