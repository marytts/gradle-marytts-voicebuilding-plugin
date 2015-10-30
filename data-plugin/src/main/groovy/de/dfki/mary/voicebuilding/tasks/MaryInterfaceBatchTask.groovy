package de.dfki.mary.voicebuilding.tasks

import groovy.io.FileType
import groovy.xml.XmlUtil

import marytts.LocalMaryInterface
import marytts.util.dom.MaryDomUtils

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class MaryInterfaceBatchTask extends DefaultTask {

    @InputDirectory
    File srcDir

    @Input
    String inputType

    @Input
    String outputType

    @Input
    String inputExt

    @Input
    String outputExt

    @OutputDirectory
    File destDir

    @TaskAction
    void process() {
        def parser = new XmlSlurper(false, false)
        def mary = new LocalMaryInterface()
        // TODO: locale must be configurable
        mary.locale = Locale.US
        mary.inputType = inputType
        mary.outputType = outputType
        def inputIsXml = mary.isXMLType(inputType)
        def outputIsXml = mary.isXMLType(outputType)
        srcDir.eachFileMatch(FileType.FILES, ~/.+\.$inputExt/) { srcFile ->
            def input = srcFile.text
            if (inputIsXml) {
                input = MaryDomUtils.parseDocument(input)
            }
            def destFile = new File(destDir, srcFile.name.replace(inputExt, outputExt))
            if (outputIsXml) {
                def doc = mary.generateXML(input)
                def xmlStr = XmlUtil.serialize(doc.documentElement)
                def xml = parser.parseText(xmlStr)
                destFile.text = XmlUtil.serialize(xml)
            } else {
                destFile.text = mary.generateText(input)
            }
        }
    }
}
