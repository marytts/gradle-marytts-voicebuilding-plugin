package de.dfki.mary.plugins.marytts.voicebuilding

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin
import org.gradle.api.tasks.Copy

class VoicebuildingPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)
        project.plugins.apply(MavenPlugin)

        project.sourceCompatibility = 1.7

        project.ext {
            generatedSrcDir = "$project.buildDir/generated-src"
            voiceNameCamelCase = project.voiceName.split(/[^_A-Za-z0-9]/).collect { it.capitalize() }.join()
        }

        addTasks(project)
    }

    private void addTasks(Project project) {
        project.task('sayHi') {
            doLast {
                println "$project.name says HI!"
            }
        }

        project.task('unpackTemplate', type: Copy) {
            from project.file(getClass().getResource("/de/dfki/mary/plugins/marytts/voicebuilding/templates/Config.java"))
            into project.generatedSrcDir
            expand project.properties
        }
    }
}
