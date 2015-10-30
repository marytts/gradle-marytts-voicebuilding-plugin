package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class LegacyVoiceImportTask extends DefaultTask {

    @Optional
    @InputDirectory
    File srcDir

    @Optional
    @InputDirectory
    File srcDir2

    @Optional
    @InputFile
    File srcFile

    @Optional
    @InputFile
    File srcFile2

    @Optional
    @InputFile
    File srcFile3

    @Optional
    @OutputDirectory
    File destDir

    @Optional
    @OutputFile
    File destFile

    @Optional
    @OutputFile
    File destFile2

    @Optional
    @OutputFile
    File destFile3

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
