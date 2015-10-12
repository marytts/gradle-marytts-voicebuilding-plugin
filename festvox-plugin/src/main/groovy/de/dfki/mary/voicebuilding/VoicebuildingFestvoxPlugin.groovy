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

        project.processDataResources {
            from project.configurations.data
            filesMatching '*.tar.bz2', { tarFileDetails ->
                project.copy {
                    from project.tarTree(tarFileDetails.file)
                    into destinationDir
                    include '**/wav/*.wav', '**/lab/*.lab', '**/etc/*.data'
                    eachFile {
                        it.path = it.name
                    }
                    includeEmptyDirs = false
                }
                tarFileDetails.exclude()
            }
        }

        project.task('text', type: FestvoxTextTask) {
            dependsOn project.processDataResources
            destDir = project.file("$project.buildDir/text")
        }
    }
}
