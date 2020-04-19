package de.dfki.mary.voicebuilding.tasks

import marytts.util.data.ESTTrackReader
import marytts.util.data.MaryHeader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.m2ci.msp.jtgt.io.XWaveLabelSerializer

class PhoneUnitFileMaker extends DefaultTask {

    @InputFile
    final RegularFileProperty basenamesFile = project.objects.fileProperty()

    @InputDirectory
    final DirectoryProperty srcDir = project.objects.directoryProperty()

    @Input
    Property<String> srcExt = project.objects.property(String)

    @InputDirectory
    final DirectoryProperty pmDir = project.objects.directoryProperty()

    @Input
    Property<Integer> sampleRate = project.objects.property(Integer)

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    @TaskAction
    void make() {
        def labSerializer = new XWaveLabelSerializer()
        def baos = new ByteArrayOutputStream()
        def out = new DataOutputStream(baos)
        def utteranceEndSample = 0
        def numUnits = 0
        basenamesFile.get().asFile.eachLine('UTF-8') { basename ->
            def pmFile = pmDir.file("${basename}.pm").get().asFile
            def pm = new ESTTrackReader(pmFile.path)
            out.writeLong(utteranceEndSample)
            out.writeInt(-1)
            numUnits++
            def srcFile = srcDir.file("${basename}.${srcExt.get()}").get().asFile
            def unitStartSample = 0
            labSerializer.fromString(srcFile.text).tiers[0].annotations.each { segment ->
                def unitEndTime = pm.getClosestTime(segment.end)
                def unitEndSample = (unitEndTime * sampleRate.get()) as long
                def unitNumSamples = unitEndSample - unitStartSample
                // TODO: skip units with zero duration
                out.writeLong(utteranceEndSample + unitStartSample)
                out.writeInt(unitNumSamples as int)
                unitStartSample = unitEndSample
                numUnits++
            }
            out.writeLong(utteranceEndSample + unitStartSample)
            out.writeInt(-1)
            numUnits++
            def utteranceEndTime = pm.timeSpan
            utteranceEndSample += (utteranceEndTime * sampleRate.get()) as long
        }
        destFile.get().asFile.withDataOutputStream { dest ->
            new MaryHeader(MaryHeader.UNITS).writeTo(dest)
            dest.writeInt(numUnits)
            dest.writeInt(sampleRate.get())
            dest.write(baos.toByteArray())
        }
    }
}
