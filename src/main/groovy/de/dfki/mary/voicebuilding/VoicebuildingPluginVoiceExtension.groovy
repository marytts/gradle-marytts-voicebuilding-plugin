package de.dfki.mary.voicebuilding

import org.gradle.api.Project

class VoicebuildingPluginVoiceExtension {
    final Project project

    def description
    def gender
    def name
    def nameCamelCase
    def language
    def locale
    def localeXml
    def maryLocale
    def maryLocaleXml
    def region
    def samplingRate
    def type
    def wantsToBeDefault

    VoicebuildingPluginVoiceExtension(final Project project) {
        this.project = project
    }

    def getLocale() {
        locale = locale ?: [language, getRegion()].join('_')
    }

    def getLocaleXml() {
        localeXml = localeXml ?: [language, getRegion()].join('-')
    }

    def getMaryLocale() {
        maryLocale = maryLocale ?: language.equalsIgnoreCase(getRegion()) ? language : getLocale()
    }

    def getMaryLocaleXml() {
        maryLocaleXml = maryLocaleXml ?: language.equalsIgnoreCase(getRegion()) ? language : getLocaleXml()
    }

    def getNameCamelCase() {
        nameCamelCase = nameCamelCase ?: name.split(/[^_A-Za-z0-9]/).collect { it.capitalize() }.join()
    }

    def getRegion() {
        region = region ?: language.toUpperCase()
    }
}
