package de.dfki.mary.voicebuilding

class VoiceExtension {

    String name
    String gender
    String domain
    String language
    String region
    String type
    int samplingRate

    VoiceExtension() {
        name = 'my_voice'
        gender = 'female'
        domain = 'general'
        language = 'en'
        region = 'US'
        type = 'unit selection'
        samplingRate = 16000
    }

    def nameCamelCase = {
        name?.split(/[^_A-Za-z0-9]/).collect { it.capitalize() }.join()
    }

    def locale = {
        locale ?: [language, region].join('_')
    }

    def localeXml = {
        [language, region].join('-')
    }

    def maryLocale = {
        language?.equalsIgnoreCase(region) ? language : locale
    }

    def maryLocaleXml = {
        language?.equalsIgnoreCase(region) ? language : localeXml
    }
}
