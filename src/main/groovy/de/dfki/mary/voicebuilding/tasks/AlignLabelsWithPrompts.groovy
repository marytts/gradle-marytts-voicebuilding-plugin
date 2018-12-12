package de.dfki.mary.voicebuilding.tasks

import groovy.xml.XmlUtil
import marytts.tools.analysis.MaryTranscriptionAligner
import marytts.util.dom.MaryDomUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.m2ci.msp.jtgt.io.XWaveLabelSerializer

class AlignLabelsWithPrompts extends DefaultTask {

    @InputFile
    final RegularFileProperty basenamesFile = newInputFile()

    @InputDirectory
    final DirectoryProperty labDir = newInputDirectory()

    @InputDirectory
    final DirectoryProperty maryXmlDir = newInputDirectory()

    @OutputDirectory
    final DirectoryProperty destDir = newOutputDirectory()

    @TaskAction
    void align() {
        def labSerializer = new XWaveLabelSerializer()
        def aligner = new MaryTranscriptionAligner()
        aligner.ensureInitialBoundary = true
        def slurper = new XmlSlurper(false, false)
        basenamesFile.get().asFile.eachLine('UTF-8') { basename ->
            def labFile = labDir.file("${basename}.lab").get().asFile
            def phoneTier = labSerializer.fromString(labFile.getText('UTF-8')).tiers.first()
            def phoneStr = phoneTier.annotations.collect { it.text }.join('|')
            def maryXmlFile = maryXmlDir.file("${basename}.xml").get().asFile
            def maryXmlDocument = MaryDomUtils.parseDocument(maryXmlFile)
            aligner.alignXmlTranscriptions(maryXmlDocument, phoneStr)
            def maryXmlOutputStream = new ByteArrayOutputStream()
            MaryDomUtils.document2Stream(maryXmlDocument, maryXmlOutputStream)
            def maryXmlReader = new StringReader(maryXmlOutputStream.toString())
            def maryXml = slurper.parse(maryXmlReader)
            def destFile = destDir.file("${basename}.xml").get().asFile
            XmlUtil.serialize(maryXml, destFile.newWriter('UTF-8'))
        }
    }
}
