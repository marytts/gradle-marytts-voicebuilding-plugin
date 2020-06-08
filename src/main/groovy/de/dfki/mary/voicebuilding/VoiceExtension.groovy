package de.dfki.mary.voicebuilding

import org.gradle.api.Project
import org.gradle.api.provider.Property

class VoiceExtension {

    Project project
    Property<String> name
    Property<String> gender
    Property<String> domain
    Property<String> language
    Property<String> region
    Property<String> type
    Property<String> description
    Property<Integer> samplingRate

    VoiceExtension(Project project) {
        this.name = project.objects.property(String)
        this.gender = project.objects.property(String)
        this.domain = project.objects.property(String)
        this.language = project.objects.property(String)
        this.region = project.objects.property(String)
        setRegion('US')
        this.type = project.objects.property(String)
        this.description = project.objects.property(String)
        this.samplingRate = project.objects.property(Integer)
        this.project = project
    }

    String getName() {
        this.name.getOrElse('my_voice')
    }

    void setName(String name) {
        this.name.set(name)
        this.project.marytts.component.name = this.nameCamelCase
        this.project.marytts.component.packageName = "marytts.voice.${this.nameCamelCase}"
    }

    String getNameCamelCase() {
        getName().split(/[^_A-Za-z0-9]/).collect { it.capitalize() }.join()
    }

    String getGender() {
        this.gender.getOrElse('female')
    }

    void setGender(String gender) {
        this.gender.set(gender)
    }

    String getDomain() {
        this.domain.getOrElse('general')
    }

    void setDomain(String domain) {
        this.domain.set(domain)
    }

    String getLanguage() {
        this.language.getOrElse('en')
    }

    void setLanguage(String language) {
        this.language.set(language)
        setRegion(null)
    }

    String getRegion() {
        this.region.getOrNull()
    }

    void setRegion(String region) {
        this.region.set(region)
    }

    String getType() {
        this.type.getOrElse('unit selection')
    }

    void setType(String type) {
        this.type.set(type)
    }

    String getDescription() {
        this.description.getOrElse(String.format('A %s %s %s voice', getGender(), getLocale().getDisplayLanguage(Locale.ENGLISH), getType()))
    }

    void setDescription(String description) {
        this.description.set(description)
    }

    Integer getSamplingRate() {
        this.samplingRate.getOrElse(16000)
    }

    void setSamplingRate(Integer samplingRate) {
        this.samplingRate.set(samplingRate)
    }

    Locale getLocale() {
        getRegion() ? new Locale(getLanguage(), getRegion()) : new Locale(getLanguage())
    }

    String getLocaleXml() {
        getLocale().toLanguageTag()
    }

    String getMaryLocale() {
        getLanguage().equalsIgnoreCase(getRegion()) ? getLanguage() : getLocale()
    }

    String getMaryLocaleXml() {
        getLanguage().equalsIgnoreCase(getRegion()) ? getLanguage() : getLocaleXml()
    }
}
