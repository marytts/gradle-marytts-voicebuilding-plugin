package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.*

import org.gradle.api.Plugin
import org.gradle.api.Project

class VoicebuildingFestvoxPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.repositories {
            ivy {
                url 'http://festvox.org/examples'
                layout 'pattern', {
                    artifact '[module]_[classifier]/packed/[artifact].tar.bz2'
                }
            }
        }

        project.plugins.apply VoicebuildingDataPlugin
    }
}
