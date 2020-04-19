package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

class TrainProsodyCart extends DefaultTask {

    @InputFile
    final RegularFileProperty wagon = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty dataFile = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty descriptionFile = project.objects.fileProperty()

    @Input
    Property<String> predictee = project.objects.property(String)

    @Optional
    @Input
    ListProperty<String> ignoreFields = project.objects.listProperty(String)

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    TrainProsodyCart() {
        def wagonPath = System.env['PATH'].split(':').collect { dir ->
            new File(dir, 'wagon')
        }.find { it.canExecute() }
        wagon.set(wagonPath)
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
