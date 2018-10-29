package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.bundling.Zip

class LegacyZip extends Zip {

    @OutputFile
    final RegularFileProperty destFile = newOutputFile()

    LegacyZip() {
        classifier 'legacy'
    }
}
