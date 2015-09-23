package de.dfki.mary.voicebuilding.tasks

import groovy.xml.XmlUtil

import marytts.LocalMaryInterface

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class AllophonesExtractorTask extends DefaultTask {

    @OutputDirectory
    File allophonesDir

    @TaskAction
    void process() {
        def parser = new XmlSlurper(false, false)
        def mary = new LocalMaryInterface()
        // TODO: locale must be configurable
        mary.locale = Locale.US
        mary.outputType = 'ALLOPHONES'
        inputs.sourceFiles.each { srcFile ->
            def doc = mary.generateXML(srcFile.text)
            def xmlStr = XmlUtil.serialize(doc.documentElement)
            def xml = parser.parseText(xmlStr)
            def destFile = new File(allophonesDir, srcFile.name.replace('.txt', '.xml'))
            destFile.text = XmlUtil.serialize(xml)
        }
    }
}
