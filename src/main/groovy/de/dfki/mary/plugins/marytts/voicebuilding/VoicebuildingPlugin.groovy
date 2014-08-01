package de.dfki.mary.plugins.marytts.voicebuilding

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin

class VoicebuildingPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)
        project.plugins.apply(MavenPlugin)
        project.sourceCompatibility = 1.7
        addTasks(project)
    }

    private void addTasks(Project project) {
        project.task('sayHi') {
            doLast {
                println "$project.name says HI!"
            }
        }
        project.task('unpackTemplate') {
            doLast {
                project.copy {
                    from project.file(getClass().getResource("/de/dfki/mary/plugins/marytts/voicebuilding/templates/templateTest.txt"))
                    into project.buildDir
                    expand project.properties
                }
            }
        }
    }
}
