package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class FeatureListerTask extends DefaultTask {

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    @TaskAction
    void generate() {
        project.javaexec {
            classpath project.configurations.marytts
            mainClass = 'marytts.FeatureLister'
            systemProperties = [
                    locale    : project.marytts.voice.maryLocale,
                    outputFile: destFile.get().asFile
            ]
        }
    }
}
