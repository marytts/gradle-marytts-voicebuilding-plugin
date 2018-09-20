package de.dfki.mary.voicebuilding.tasks

import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class LegacyDescriptorTask extends DefaultTask {

    @InputFile
    File srcFile

    @OutputFile
    File destFile

    @TaskAction
    void generate() {
        project.ant.checksum file: srcFile, algorithm: 'MD5', property: 'srcFileHash'
        def builder = new StreamingMarkupBuilder()
        def xml = builder.bind {
            'marytts-install'(xmlns: 'http://mary.dfki.de/installer') {
                voice(gender: project.marytts.voice.gender, locale: project.marytts.voice.maryLocale, name: project.marytts.voice.name, type: project.marytts.voice.type, version: project.version) {
                    delegate.description project.marytts.voice.description
                    license(href: project.marytts.voice.license.url)
                    'package'(filename: srcFile.name, md5sum: project.ant.srcFileHash, size: srcFile.size()) {
                        location(folder: true, href: "http://mary.dfki.de/download/$project.marytts.version/")
                    }
                    depends(language: project.marytts.voice.maryLocaleXml, version: project.marytts.version)
                }
            }
        }
        destFile.text = XmlUtil.serialize(xml)
    }
}
