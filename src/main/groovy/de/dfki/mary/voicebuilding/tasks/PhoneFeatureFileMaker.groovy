package de.dfki.mary.voicebuilding.tasks

import marytts.features.FeatureDefinition
import marytts.unitselection.data.UnitFileReader
import marytts.util.data.MaryHeader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

class PhoneFeatureFileMaker extends DefaultTask {

    @InputFile
    final RegularFileProperty basenamesFile = project.objects.fileProperty()

    @InputDirectory
    final DirectoryProperty srcDir = project.objects.directoryProperty()

    @Input
    Property<String> srcExt = project.objects.property(String)

    @InputFile
    final RegularFileProperty unitFile = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty featureDefinitionFile = project.objects.fileProperty()

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    @TaskAction
    void make() {
        destFile.get().asFile.withDataOutputStream { dest ->
            def header = new MaryHeader(MaryHeader.UNITFEATS)
            header.writeTo(dest)
            def featureDefinition = new FeatureDefinition(featureDefinitionFile.get().asFile.newReader('UTF-8'), true)
            featureDefinition.writeBinaryTo(dest)
            def units = new UnitFileReader(unitFile.get().asFile.path)
            def numUnits = units.numberOfUnits
            dest.writeInt(numUnits)
            basenamesFile.get().asFile.eachLine('UTF-8') { basename ->
                featureDefinition.createEdgeFeatureVector(0, true).writeTo(dest)
                def srcFile = srcDir.file("${basename}.${srcExt.get()}").get().asFile
                def featureFileBlock = 0
                srcFile.eachLine('UTF-8') { line ->
                    if (line.trim().isEmpty()) {
                        featureFileBlock++
                    } else if (featureFileBlock == 2) {
                        try {
                            featureDefinition.toFeatureVector(0, line).writeTo(dest)
                        } catch (ex) {
                            println line
                            println featureDefinition.toString()
                            throw ex
                        }
                    }
                }
                featureDefinition.createEdgeFeatureVector(0, false).writeTo(dest)
            }
        }
    }
}
