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
                voice(gender: project.voice.gender, locale: project.voice.maryLocale, name: project.voice.name, type: project.voice.type, version: project.maryttsVersion) {
                    delegate.description project.voice.description
                    license(href: project.voice.license.url)
                    'package'(filename: srcFile.name, md5sum: project.ant.srcFileHash, size: srcFile.size()) {
                        location(folder: true, href: "http://mary.dfki.de/download/$project.maryttsVersion/")
                    }
                    depends(language: project.voice.maryLocaleXml, version: project.maryttsVersion)
                }
            }
        }
        destFile.text = XmlUtil.serialize(xml)
    }
}
