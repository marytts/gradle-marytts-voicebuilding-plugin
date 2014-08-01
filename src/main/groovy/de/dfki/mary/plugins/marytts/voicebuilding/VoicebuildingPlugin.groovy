package de.dfki.mary.plugins.marytts.voicebuilding

import org.gradle.api.Plugin
import org.gradle.api.Project

class VoicebuildingPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        addTasks(project)
    }

    private void addTasks(Project project) {
        project.task('sayHi') {
            doLast {
                println "$project.name says HI!"
            }
        }
    }
}
