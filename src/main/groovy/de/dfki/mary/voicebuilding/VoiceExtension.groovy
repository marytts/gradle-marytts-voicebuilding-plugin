package de.dfki.mary.voicebuilding

class VoiceExtension {

    String name
    String gender
    String domain
    String language
    String region
    String type
    String description
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

    String getNameCamelCase() {
        name?.split(/[^_A-Za-z0-9]/).collect { it.capitalize() }.join()
    }

    void setLanguage(String newLanguage) {
        language = newLanguage
        region = null
    }

    Locale getLocale() {
        region ? new Locale(language, region) : new Locale(language)
    }

    String getLocaleXml() {
        locale.toLanguageTag()
    }

    String getMaryLocale() {
        language?.equalsIgnoreCase(region) ? language : locale
    }

    String getMaryLocaleXml() {
        language?.equalsIgnoreCase(region) ? language : localeXml
    }

    String getDescription() {
        description ?: "A $gender ${locale.getDisplayLanguage(Locale.ENGLISH)} $type voice"
    }
}
