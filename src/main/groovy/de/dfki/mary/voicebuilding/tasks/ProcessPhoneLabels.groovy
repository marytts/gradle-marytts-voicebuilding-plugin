package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.m2ci.msp.jtgt.TextGrid
import org.m2ci.msp.jtgt.io.XWaveLabelSerializer

class ProcessPhoneLabels extends DefaultTask {

    @InputFile
    final RegularFileProperty basenamesFile = project.objects.fileProperty()

    @InputDirectory
    final DirectoryProperty srcDir = project.objects.directoryProperty()

    @OutputDirectory
    final DirectoryProperty destDir = project.objects.directoryProperty()

    @TaskAction
    void process() {
        def labSerializer = new XWaveLabelSerializer()
        def tierName = 'phones'
        basenamesFile.get().asFile.eachLine('UTF-8') { basename ->
            def srcFile = srcDir.file("${basename}.lab").get().asFile
            def phoneTier = labSerializer.fromString(srcFile.getText('UTF-8')).tiers.first()
            def phoneIterator = phoneTier.annotations.listIterator()
            def phone = phoneIterator.next()
            while (phoneIterator.hasNext()) {
                def nextPhone = phoneIterator.next()
                // merge duplicate pauses
                if (phone.text == nextPhone.text && phone.text == '_') {
                    phone.end = nextPhone.end
                    phoneIterator.remove()
                }
                phone = nextPhone
            }
            destDir.file("${basename}.lab").get().asFile.withWriter('UTF-8') { dest ->
                def textGrid = new TextGrid(phoneTier.start, phoneTier.end, [phoneTier])
                dest << labSerializer.toString(textGrid, tierName)
            }
        }
    }
}
