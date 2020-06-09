package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

class GenerateVoiceSource extends DefaultTask {

    @OutputFile
    final RegularFileProperty configTestFile = project.objects.fileProperty()

    @OutputFile
    final RegularFileProperty integrationTestFile = project.objects.fileProperty()

    @TaskAction
    void generate() {
        def engine = new groovy.text.GStringTemplateEngine()
        def binding = [ project: project ]

        def f = new InputStreamReader(getClass().getResourceAsStream('ConfigTest.java'))
        def template = engine.createTemplate(f).make(binding)
        configTestFile.get().asFile.text = template.toString()

        f = new InputStreamReader(getClass().getResourceAsStream('LoadVoiceIT.groovy'))
        template = engine.createTemplate(f).make(binding)
        integrationTestFile.get().asFile.text = template.toString()
    }
}
