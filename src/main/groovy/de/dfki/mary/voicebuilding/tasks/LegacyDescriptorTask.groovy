package de.dfki.mary.voicebuilding.tasks

import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class LegacyDescriptorTask extends DefaultTask {

    @InputFile
    final RegularFileProperty srcFile = newInputFile()

    @OutputFile
    final RegularFileProperty destFile = newOutputFile()

    @TaskAction
    void generate() {
        project.ant.checksum file: srcFile.get().asFile, algorithm: 'MD5', property: 'srcFileHash'
        def builder = new StreamingMarkupBuilder()
        def xml = builder.bind {
            'marytts-install'(xmlns: 'http://mary.dfki.de/installer') {
                voice(gender: project.marytts.voice.gender, locale: project.marytts.voice.maryLocale, name: project.marytts.voice.name, type: project.marytts.voice.type, version: project.version) {
                    delegate.description project.marytts.voice.description
                    license(href: project.marytts.voice.license.url)
                    'package'(filename: srcFile.get().asFile.name, md5sum: project.ant.srcFileHash, size: srcFile.get().asFile.size()) {
                        location(folder: true, href: "http://mary.dfki.de/download/$project.marytts.version/")
                    }
                    depends(language: project.marytts.voice.maryLocaleXml, version: project.marytts.version)
                }
            }
        }
        destFile.get().asFile.setText(XmlUtil.serialize(xml), 'UTF-8')
    }
}
