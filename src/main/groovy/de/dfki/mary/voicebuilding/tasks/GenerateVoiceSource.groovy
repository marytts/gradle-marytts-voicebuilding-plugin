package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*

class GenerateVoiceSource extends DefaultTask {

    @OutputDirectory
    final DirectoryProperty testDirectory = project.objects.directoryProperty()

    @OutputDirectory
    final DirectoryProperty integrationTestDirectory = project.objects.directoryProperty()

    @TaskAction
    void generate() {
        def engine = new groovy.text.GStringTemplateEngine()
        def binding = [ project: project ]

        def templateStream = new InputStreamReader(getClass().getResourceAsStream('ConfigTest.java'))
        def template = engine.createTemplate(templateStream).make(binding)
        def configTestFile = new File(testDirectory.get().asFile, "${project.marytts.component.packagePath}/VoiceConfigTest.groovy")
        configTestFile.parentFile.mkdirs()
        configTestFile.text = template.toString()

        templateStream = new InputStreamReader(getClass().getResourceAsStream('LoadVoiceIT.groovy'))
        template = engine.createTemplate(templateStream).make(binding)
        def integrationTestFile = new File(integrationTestDirectory.get().asFile, "${project.marytts.component.packagePath}/LoadVoiceIT.groovy")
        integrationTestFile.parentFile.mkdirs()
        integrationTestFile.text = template.toString()
    }
}
