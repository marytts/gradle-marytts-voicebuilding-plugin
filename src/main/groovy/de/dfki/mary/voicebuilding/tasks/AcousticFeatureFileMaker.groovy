package de.dfki.mary.voicebuilding.tasks

import marytts.features.FeatureDefinition
import marytts.features.FeatureVector
import marytts.unitselection.data.FeatureFileReader
import marytts.unitselection.data.Unit
import marytts.unitselection.data.UnitFileReader
import marytts.util.data.MaryHeader
import marytts.util.math.Polynomial
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class AcousticFeatureFileMaker extends DefaultTask {

    @InputFile
    final RegularFileProperty featureDefinitionFile = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty unitFile = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty contourFile = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty featureFile = project.objects.fileProperty()

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    @TaskAction
    void make() {
        def units = new UnitFileReader(unitFile.get().asFile.path)
        def sampleRate = units.sampleRate as float
        def contours = new FeatureFileReader(contourFile.get().asFile.path)
        def features = new FeatureFileReader(featureFile.get().asFile.path)
        def writer = new StringWriter()
        features.featureDefinition.writeTo(new PrintWriter(writer), true)
        writer.println '100 linear | unit_duration'
        writer.println '100 linear | unit_logf0'
        writer.println '0 linear | unit_logf0delta'
        def reader = new StringReader(writer.toString())
        def featureDefinition = new FeatureDefinition(new BufferedReader(reader), true)
        destFile.get().asFile.withDataOutputStream { dest ->
            new MaryHeader(MaryHeader.UNITFEATS).writeTo(dest)
            featureDefinition.writeBinaryTo(dest)

            int numUnits = units.numberOfUnits
            dest.writeInt(numUnits)
            int fiPhoneme = featureDefinition.getFeatureIndex("phone")
            byte fvPhoneme_0 = featureDefinition.getFeatureValueAsByte(fiPhoneme, "0")
            byte fvPhoneme_Silence = featureDefinition.getFeatureValueAsByte(fiPhoneme, "_")
            int fiVowel = featureDefinition.getFeatureIndex("ph_vc")
            byte fvVowel = featureDefinition.getFeatureValueAsByte(fiVowel, "+")
            int fiLR = featureDefinition.getFeatureIndex("halfphone_lr")
            byte fvLR_L = featureDefinition.getFeatureValueAsByte(fiLR, "L")
            byte fvLR_R = featureDefinition.getFeatureValueAsByte(fiLR, "R")
            int fiSylStart = featureDefinition.getFeatureIndex("segs_from_syl_start")
            int fiSylEnd = featureDefinition.getFeatureIndex("segs_from_syl_end")
            int iSylVowel = -1
            List<Float> unitDurs = []

            int iCurrent = 0
            for (int i = 0; i < units.numberOfUnits; i++) {
                FeatureVector inFV = features.getFeatureVector(i)
                Unit u = units.getUnit(i)
                float dur = u.duration / sampleRate

                // No syllable structure for edge and silence phone entries:
                if (inFV.getByteFeature(fiPhoneme) == fvPhoneme_0 || inFV.getByteFeature(fiPhoneme) == fvPhoneme_Silence) {
                    unitDurs << dur
                    continue
                }
                // Else, unit belongs to a syllable
                if (inFV.getByteFeature(fiSylStart) == 0 && inFV.getByteFeature(fiLR) == fvLR_L) {
                    // first segment in syllable
                    if (iCurrent < i) { // Something to output before this syllable
                        assert i - iCurrent == unitDurs.size()
                        writeFeatureVectors(dest, iCurrent, iSylVowel, i - 1, unitDurs, contours, features, featureDefinition)
                    }
                    unitDurs.clear()
                    iSylVowel = -1
                    iCurrent = i
                }

                unitDurs << dur

                if (inFV.getByteFeature(fiVowel) == fvVowel && iSylVowel == -1) { // the first vowel in the syllable
                    iSylVowel = i
                }

                if (inFV.getByteFeature(fiSylEnd) == 0 && inFV.getByteFeature(fiLR) == fvLR_R) {
                    // last segment in syllable
                    writeFeatureVectors(dest, iCurrent, iSylVowel, i, unitDurs, contours, features, featureDefinition)
                    iSylVowel = -1
                    unitDurs.clear()
                    iCurrent = i + 1
                }
            }

            assert numUnits - iCurrent == unitDurs.size()
            writeFeatureVectors(dest, iCurrent, iSylVowel, numUnits - 1, unitDurs, contours, features, featureDefinition)
        }
    }

    void writeFeatureVectors(DataOutput out, int iFirst, int iVowel, int iLast, List<Float> unitDurs, FeatureFileReader contours, FeatureFileReader features, FeatureDefinition featureDefinition) throws IOException {
        float[] coeffs
        if (iVowel != -1) { // Syllable contains a vowel
            coeffs = contours.getFeatureVector(iVowel).continuousFeatures
            boolean isZero = true
            for (int c = 0; c < coeffs.length; c++) {
                if (coeffs[c] != 0) {
                    isZero = false
                    break
                }
            }
            if (isZero) {
                coeffs = null
            }
        }
        assert unitDurs.size() == iLast - iFirst + 1

        float sylDur = 0
        for (int i = 0; i < unitDurs.size(); i++) {
            sylDur += unitDurs.get(i)
        }

        float uStart = 0
        for (int i = 0; iFirst + i <= iLast; i++) {
            float logF0 = Float.NaN
            float logF0delta = Float.NaN
            if (coeffs && unitDurs.get(i) > 0) {
                float relUStart = uStart / sylDur // in [0, 1[
                float relUEnd = (uStart + unitDurs.get(i)) / sylDur // in [0, 1[
                double[] predUnitContour = Polynomial.generatePolynomialValues(coeffs as double[], 10, relUStart, relUEnd)
                // And fit a linear curve to this:
                double[] unitCoeffs = Polynomial.fitPolynomial(predUnitContour, 1)
                assert unitCoeffs.length == 2
                // unitCoeffs[0] is the slope, unitCoeffs[1] the value at left end of interval.
                // We need the f0 value in the middle of the unit:
                logF0 = (float) (unitCoeffs[1] + 0.5 * unitCoeffs[0])
                logF0delta = (float) unitCoeffs[0]
            }

            FeatureVector fv = features.getFeatureVector(iFirst + i)
            String line = fv.toString() + " " + unitDurs.get(i) + " " + logF0 + " " + logF0delta
            FeatureVector outFV = featureDefinition.toFeatureVector(0, line)
            outFV.writeTo(out)
            uStart += unitDurs.get(i)
        }
    }
}
