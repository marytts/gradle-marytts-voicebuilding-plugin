package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.provider.ListProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

interface RunnableParameters extends WorkParameters {
    ListProperty<String> getCommandLine()
}

abstract class RunnableExec implements WorkAction<RunnableParameters> {

    @Override
    void execute() {
        println parameters.commandLine.get().join(' ')
        def proc = parameters.commandLine.get().execute()
        proc.waitFor()
        assert proc.exitValue() == 0: proc.errorStream.text
    }
}
