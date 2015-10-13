package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*

class FestvoxLabTask extends Copy {

    def mapping = [
            'aa' : 'A',
            'ae' : '{',
            'ah' : 'V',
            'ao' : 'O',
            'aw' : 'aU',
            'ax' : '@',
            'ay' : 'AI',
            'ch' : 'tS',
            'dh' : 'D',
            'eh' : 'E',
            'er' : 'r=',
            'ey' : 'EI',
            'hh' : 'h',
            'ih' : 'I',
            'iy' : 'i',
            'jh' : 'dZ',
            'ng' : 'N',
            'ow' : '@U',
            'oy' : 'OI',
            'pau': '_',
            'sh' : 'S',
            'th' : 'T',
            'uh' : 'U',
            'uw' : 'u',
            'y'  : 'j',
            'zh' : 'Z'
    ]

    FestvoxLabTask() {
        include '*.lab'
        filter {
            it.replaceAll(/\w+$/) {
                mapping[it] ?: it
            }
        }
    }
}
