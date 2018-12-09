package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@Deprecated
class LegacyVoiceImportTask extends DefaultTask {

    @InputFile
    final RegularFileProperty configFile = newInputFile()

    @InputFile
    final RegularFileProperty basenamesFile = newInputFile()

    @Optional
    @InputDirectory
    final DirectoryProperty srcDir = newInputDirectory()

    @Optional
    @InputDirectory
    final DirectoryProperty srcDir2 = newInputDirectory()

    @Optional
    @InputDirectory
    final DirectoryProperty srcDir3 = newInputDirectory()

    @Optional
    @InputFile
    final RegularFileProperty srcFile = newInputFile()

    @Optional
    @InputFile
    final RegularFileProperty srcFile2 = newInputFile()

    @Optional
    @InputFile
    final RegularFileProperty srcFile3 = newInputFile()

    @Optional
    @OutputDirectory
    final DirectoryProperty destDir = newOutputDirectory()

    @Optional
    @OutputFile
    final RegularFileProperty destFile = newOutputFile()

    @Optional
    @OutputFile
    final RegularFileProperty destFile2 = newOutputFile()

    @Optional
    @OutputFile
    final RegularFileProperty destFile3 = newOutputFile()

    @TaskAction
    void run() {
        project.javaexec {
            classpath project.configurations.legacy, project.configurations.compile
            main 'marytts.tools.voiceimport.DatabaseImportMain'
            workingDir project.buildDir
            args name - 'legacy'
            systemProperties = ['java.awt.headless': true]
        }
    }
}
