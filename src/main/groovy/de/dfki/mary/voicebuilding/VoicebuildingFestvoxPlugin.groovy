package de.dfki.mary.voicebuilding

import de.dfki.mary.voicebuilding.tasks.*

import org.gradle.api.Plugin
import org.gradle.api.Project

class VoicebuildingFestvoxPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
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

        project.task('text', type: FestvoxExtractText) {
            dependsOn project.tasks.findByName('processDataResources')
            srcFile = project.file("$project.sourceSets.data.output.resourcesDir/txt.done.data")
            destDir = project.layout.buildDirectory.dir('text')
        }

        project.task('lab', type: FestvoxExtractLab) {
            srcFiles = project.files(project.tasks.getByName('processDataResources'))
            destDir = project.layout.buildDirectory.dir('lab')
            mapping = [
                    aa  : 'A',
                    ae  : '{',
                    ah  : 'V',
                    ao  : 'O',
                    aw  : 'aU',
                    ax  : '@',
                    ay  : 'AI',
                    ch  : 'tS',
                    dh  : 'D',
                    eh  : 'E',
                    er  : 'r=',
                    ey  : 'EI',
                    hh  : 'h',
                    ih  : 'I',
                    iy  : 'i',
                    jh  : 'dZ',
                    ng  : 'N',
                    ow  : '@U',
                    oy  : 'OI',
                    pau : '_',
                    sh  : 'S',
                    ssil: '_',
                    th  : 'T',
                    uh  : 'U',
                    uw  : 'u',
                    y   : 'j',
                    zh  : 'Z'
            ]
        }
    }
}
