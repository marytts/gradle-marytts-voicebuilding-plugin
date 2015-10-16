package de.dfki.mary.voicebuilding.tasks

import groovy.io.FileType

import marytts.LocalMaryInterface
import marytts.util.dom.DomUtils

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class LegacyUnitFeatureComputerTask extends DefaultTask {

    @InputFile
    File featureFile = project.legacyFeatureLister.destFile

    @Input
    String rootFeature

    @Input
    List<String> exclude

    @Input
    String outputType

    @InputDirectory
    File srcDir

    @OutputDirectory
    File destDir

    @Input
    String fileExt

    @TaskAction
    void process() {
        // list features
        def features = [rootFeature] + featureFile.readLines().findAll { it != rootFeature && !(it in exclude) }

        // init mary
        def mary = new LocalMaryInterface()
        mary.inputType = 'ALLOPHONES'
        mary.outputType = outputType
        mary.outputTypeParams = features.join(' ')
        mary.locale = project.voice.locale

        // process
        srcDir.eachFileMatch(FileType.FILES, ~/.+\.xml/) { srcFile ->
            def doc = DomUtils.parseDocument(srcFile)
            def destFile = new File(destDir, srcFile.name - 'xml' + fileExt)
            destFile.text = mary.generateText(doc)
        }
    }
}
