package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.LegacyTemplateTask

import org.gradle.api.Plugin
import org.gradle.api.Project

class VoicebuildingLegacyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.task('templates', type: LegacyTemplateTask) {
            destDir = project.file("$project.buildDir/templates")
        }
    }
}
