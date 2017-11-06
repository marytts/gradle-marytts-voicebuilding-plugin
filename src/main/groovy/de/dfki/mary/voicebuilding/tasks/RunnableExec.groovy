package de.dfki.mary.voicebuilding.tasks

import javax.inject.Inject

class RunnableExec implements Runnable {

    Map args

    @Inject
    RunnableExec(Map args) {
        this.args = args
    }

    @Override
    void run() {
        println args.commandLine.join(' ')
        args.commandLine.execute().waitFor()
    }
}
