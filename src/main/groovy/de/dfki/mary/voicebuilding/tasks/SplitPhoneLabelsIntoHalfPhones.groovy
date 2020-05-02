package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.m2ci.msp.jtgt.TextGrid
import org.m2ci.msp.jtgt.annotation.IntervalAnnotation
import org.m2ci.msp.jtgt.io.XWaveLabelSerializer
import org.m2ci.msp.jtgt.tier.IntervalTier

class SplitPhoneLabelsIntoHalfPhones extends DefaultTask {

    @InputFile
    final RegularFileProperty basenamesFile = project.objects.fileProperty()

    @InputDirectory
    final DirectoryProperty srcDir = project.objects.directoryProperty()

    @OutputDirectory
    final DirectoryProperty destDir = project.objects.directoryProperty()

    @TaskAction
    void split() {
        def labSerializer = new XWaveLabelSerializer()
        def tierName = 'halfphones'
        basenamesFile.get().asFile.eachLine('UTF-8') { basename ->
            def srcFile = srcDir.file("${basename}.lab").get().asFile
            def phoneTier = labSerializer.fromString(srcFile.getText('UTF-8')).tiers.first()
            def halfPhoneTier = new IntervalTier(tierName, 0.0, 0.0, [])
            phoneTier.annotations.each { phone ->
                def phoneMid = (phone.start + phone.end) / 2
                def leftHalfPhone = new IntervalAnnotation(phone.start, phoneMid, "${phone.text}_L")
                def rightHalfPhone = new IntervalAnnotation(phoneMid, phone.end, "${phone.text}_R")
                halfPhoneTier.addAnnotation(leftHalfPhone)
                halfPhoneTier.addAnnotation(rightHalfPhone)
            }
            destDir.file("${basename}.hplab").get().asFile.withWriter('UTF-8') { dest ->
                def textGrid = new TextGrid(halfPhoneTier.start, halfPhoneTier.end, [halfPhoneTier])
                dest << labSerializer.toString(textGrid, tierName)
            }
        }
    }
}
