package de.dfki.mary.voicebuilding.tasks

import groovy.io.FileType
import groovy.xml.XmlUtil

import marytts.LocalMaryInterface
import marytts.datatypes.MaryDataType

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class AllophonesExtractorTask extends DefaultTask {

    @InputDirectory
    File srcDir

    @OutputDirectory
    File destDir

    @TaskAction
    void process() {
        def parser = new XmlSlurper(false, false)
        def mary = new LocalMaryInterface()
        // TODO: locale must be configurable
        mary.locale = Locale.US
        mary.outputType = MaryDataType.ALLOPHONES
        srcDir.eachFileMatch(FileType.FILES, ~/.+\.txt/) { srcFile ->
            def doc = mary.generateXML(srcFile.text)
            def xmlStr = XmlUtil.serialize(doc.documentElement)
            def xml = parser.parseText(xmlStr)
            def destFile = new File(destDir, srcFile.name.replace('.txt', '.xml'))
            destFile.text = XmlUtil.serialize(xml)
        }
    }
}
