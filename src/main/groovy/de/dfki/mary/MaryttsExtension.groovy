package de.dfki.mary

import org.gradle.api.Project
import org.gradle.api.provider.Property

class MaryttsExtension {

    Property<String> version

    MaryttsExtension(Project project) {
        this.version = project.objects.property(String)
    }

    String getVersion() {
        this.version.get()
    }

    void setVersion(String version) {
        this.version.set(version)
    }
}
