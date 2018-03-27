package de.dfki.mary.voicebuilding

import org.gradle.api.Project
import org.gradle.api.provider.Property

class VoiceLicenseExtension {

    Property<String> name
    Property<String> shortName
    Property<String> url

    VoiceLicenseExtension(Project project) {
        name = project.objects.property(String)
        shortName = project.objects.property(String)
        url = project.objects.property(String)
    }

    String getName() {
        this.name.getOrElse('Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International')
    }

    void setName(String name) {
        this.name.set(name)
    }

    String getShortName() {
        this.shortName.getOrElse('CC BY-NC-SA 4.0')
    }

    void setShortName(String shortName) {
        this.shortName.set(shortName)
    }

    String getUrl() {
        this.url.getOrElse('http://creativecommons.org/licenses/by-nc-sa/4.0/')
    }

    void setUrl(String url) {
        this.url.set(url)
    }
}
