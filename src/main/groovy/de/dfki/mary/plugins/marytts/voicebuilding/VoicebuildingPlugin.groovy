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
            voiceNameCamelCase = project.voiceName.split(/[^_A-Za-z0-9]/).collect { it.capitalize() }.join()
            generatedSrcDir = "$project.buildDir/generated-src"
            generatedTestSrcDir = "$project.buildDir/generated-test-src"
        }

        addTasks(project)
    }

    private void addTasks(Project project) {
        project.task('generateSource', type: Copy) {
            from project.file(getClass().getResource("/de/dfki/mary/plugins/marytts/voicebuilding/templates/Config.java"))
            into project.generatedSrcDir
            expand project.properties
            rename { "marytts/voice/$project.voiceNameCamelCase/$it" }
        }

        project.task('generateTestSource', type: Copy) {
            from project.file(getClass().getResource("/de/dfki/mary/plugins/marytts/voicebuilding/templates/ConfigTest.java"))
            from project.file(getClass().getResource("/de/dfki/mary/plugins/marytts/voicebuilding/templates/LoadVoiceIT.java"))
            into project.generatedTestSrcDir
            expand project.properties
            rename { "marytts/voice/$project.voiceNameCamelCase/$it" }
        }

        project.processResources.doLast {
            // generate voice config
            project.copy {
                from project.file(getClass().getResource("/de/dfki/mary/plugins/marytts/voicebuilding/templates/voice${project.voiceType == "hsmm" ? "-hsmm" : ""}.config"))
                into "$destinationDir/marytts/voice/$project.voiceNameCamelCase"
                expand project.properties
            }
            // generate service loader
            project.copy {
                from project.file(getClass().getResource("/de/dfki/mary/plugins/marytts/voicebuilding/templates/marytts.config.MaryConfig"))
                into "$destinationDir/META-INF/services"
                expand project.properties
            }
        }
    }
}
