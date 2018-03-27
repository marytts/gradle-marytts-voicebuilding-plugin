package de.dfki.mary.voicebuilding

import org.gradle.api.Project

class VoicebuildingExtension {

    Project project

    List<String> basenames

    VoicebuildingExtension(Project project) {
        this.project = project
    }

    List<String> getBasenames() {
        def basenamesListFile = project.file("$project.buildDir/basenames.lst")
        if (basenamesListFile.canRead()) {
            return basenamesListFile.readLines().findAll { !it.trim().startsWith('#') }
        }
        return project.fileTree(project.buildDir).include('text/*.txt').collect { it.name - '.txt' }
    }
}
