package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.internal.os.OperatingSystem

class TrainProsodyCart extends DefaultTask {

    @InputFile
    final RegularFileProperty wagon = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty dataFile = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty descriptionFile = project.objects.fileProperty()

    @Input
    final Property<String> predictee = project.objects.property(String)

    @Optional
    @Input
    final ListProperty<String> ignoreFields = project.objects.listProperty(String)

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    TrainProsodyCart() {
        def binaryName = 'wagon'
        def pathEnv = System.env.PATH
        if (OperatingSystem.current().isWindows()) {
            binaryName += '.exe'
            pathEnv = System.env.Path
        }
        def binaryPath = pathEnv.split(File.pathSeparator).collect { dir ->
            new File(dir, binaryName)
        }.find { it.canExecute() }
        assert binaryPath: "Could not find PATH to $binaryName"
        wagon.set(binaryPath)
    }

    @TaskAction
    void train() {
        assert wagon.get().asFile.canExecute()
        project.exec {
            commandLine wagon.get().asFile,
                    '-data', dataFile.get().asFile,
                    '-desc', descriptionFile.get().asFile,
                    '-predictee', predictee.get(),
                    '-ignore', "(${ignoreFields.getOrElse([]).join(' ')})",
                    '-stop', 10,
                    '-output', destFile.get().asFile
        }
    }
}
