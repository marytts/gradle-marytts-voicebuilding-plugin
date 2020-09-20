package de.dfki.mary.voicebuilding.tasks

import marytts.features.FeatureDefinition
import marytts.features.FeatureVector
import marytts.signalproc.analysis.F0TrackerAutocorrelationHeuristic
import marytts.signalproc.analysis.PitchFileHeader
import marytts.unitselection.data.*
import marytts.util.Pair
import marytts.util.data.BufferedDoubleDataSource
import marytts.util.data.Datagram
import marytts.util.data.DatagramDoubleDataSource
import marytts.util.data.MaryHeader
import marytts.util.math.ArrayUtils
import marytts.util.math.Polynomial
import marytts.util.signal.SignalProcUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class F0ContourFeatureFileMaker extends DefaultTask {

    @InputFile
    final RegularFileProperty unitFile = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty featureFile = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty timelineFile = project.objects.fileProperty()

    @Input
    final Property<String> gender = project.objects.property(String)

    @OutputFile
    final RegularFileProperty destFile = project.objects.fileProperty()

    @TaskAction
    void make() {
        def units = new UnitFileReader(unitFile.get().asFile.path)
        destFile.get().asFile.withDataOutputStream { dest ->
            new MaryHeader(MaryHeader.UNITFEATS).writeTo(dest)
            def writer = StringWriter.newInstance()
            writer.println FeatureDefinition.BYTEFEATURES
            writer.println FeatureDefinition.SHORTFEATURES
            writer.println FeatureDefinition.CONTINUOUSFEATURES
            def polynomialOrder = 3
            polynomialOrder.downto(0) { p ->
                writer.println "0 linear | f0contour_a$p"
            }
            def reader = new BufferedReader(new StringReader(writer.toString()))
            def featureDefinition = new FeatureDefinition(reader, true)
            featureDefinition.writeBinaryTo(dest)
            writeUnitFeaturesTo(dest, polynomialOrder, units, featureDefinition)
        }
    }

    /**
     * Compute the polynomial unit features and write them to the given data output.
     *
     * @param out
     *            out
     * @throws IOException*             IOException
     * @throws UnsupportedEncodingException*             UnsupportedEncodingException
     * @throws FileNotFoundException*             FileNotFoundException
     */
    protected void writeUnitFeaturesTo(DataOutput out, int polynomOrder, UnitFileReader units, FeatureDefinition outFeatureDefinition) throws IOException, UnsupportedEncodingException, FileNotFoundException {

        def features = FeatureFileReader.getFeatureFileReader(featureFile.get().asFile.path)
        def audio = new TimelineReader(timelineFile.get().asFile.path)

        int numUnits = units.getNumberOfUnits();
        float[] zeros = new float[polynomOrder + 1];
        int unitIndex = 0;

        out.writeInt(numUnits);
        project.logger.debug("Number of units : " + numUnits);

        long startTime = System.currentTimeMillis();

        // Overall strategy:
        // 1. Go through the units sentence by sentence
        // 2. For every sentence, get the f0 contour
        // 3. For every syllable in the sentence, fit a polynomial to the f0 contour

        // 1. Go through the units sentence by sentence
        Iterator<Sentence> sit = new SentenceIterator(features);
        while (sit.hasNext()) {
            Sentence s = sit.next();

            // 2. For every sentence, get the f0 contour
            double f0FrameSkip = 0.005; // 5 ms
            double[] rawLogF0 = getLogF0Contour(s, f0FrameSkip, audio, units);
            // TODO: act appropriately if rawLogF0 is null
            double[] logF0;
            logF0 = getInterpolatedLogF0Contour(rawLogF0);

            // 3. For every syllable in the sentence, fit a polynomial to the f0 contour
            List<Polynomial> polynomials = fitPolynomialsToSyllables(s, logF0, polynomOrder, units);
            // Now save the coefficients as features of the respective syllable nucleus
            Iterator<Polynomial> polyIt = polynomials.iterator();
            for (Syllable syl : s) {
                assert polyIt.hasNext();
                Polynomial poly = polyIt.next();
                int iSylNucleus = syl.getSyllableNucleusIndex();
                // We write the polynomial coefficients as features of the nucleus, and zeros for the other units.
                // First the zeros:
                while (unitIndex < iSylNucleus) {
                    FeatureVector outFV = outFeatureDefinition.toFeatureVector(unitIndex, null, null, zeros);
                    outFV.writeTo(out);
                    unitIndex++;
                }
                // And now the nucleus:
                float[] fcoeffs;
                if (poly == null) {
                    fcoeffs = zeros;
                } else {
                    fcoeffs = ArrayUtils.copyDouble2Float(poly.coeffs);
                }
                FeatureVector outFV = outFeatureDefinition.toFeatureVector(unitIndex, null, null, fcoeffs);
                outFV.writeTo(out);
                unitIndex++;
            }
            assert !polyIt.hasNext(); // as many polynomials as syllables

        }
        // Write any trailing zeros after last syllable nucleus of last sentence:
        while (unitIndex < numUnits) {
            FeatureVector outFV = outFeatureDefinition.toFeatureVector(unitIndex, null, null, zeros);
            outFV.writeTo(out);
            unitIndex++;
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Processed " + numUnits + " units in " + (endTime - startTime) + " ms");

    }

    /**
     * For the given sentence, obtain a log f0 contour. This implementation computes the f0 contour on the fly, using a built-in
     * F0 tracker. If config setting {@see # INTERPOLATE} is true, interpolate the curve across unvoiced sections, else unvoiced
     * sections will be NaN.
     *
     * @param s
     *            the current sentence
     * @param skipSizeInSeconds
     *            the sampling frequency of the F0 contour to return (e.g., 0.005 to get a value every 5 ms)
     * @return a double array representing the F0 contour, sampled at skipSizeInSeconds, or null if no f0 contour could be
     *         computed
     * @throws IOException*             if there is a problem reading the audio data
     */
    private double[] getLogF0Contour(Sentence s, double skipSizeInSeconds, TimelineReader audio, UnitFileReader units) throws IOException {
        PitchFileHeader params = new PitchFileHeader();
        params.fs = audio.getSampleRate();
        def minPitch = gender.get() == 'female' ? 100 : 75
        def maxPitch = gender.get() == 'female' ? 500 : 300
        params.minimumF0 = minPitch;
        params.maximumF0 = maxPitch;
        params.skipSizeInSeconds = skipSizeInSeconds;
        F0TrackerAutocorrelationHeuristic tracker = new F0TrackerAutocorrelationHeuristic(params);
        double[] sentenceAudio = getAudio(s, audio, units);
        tracker.pitchAnalyze(new BufferedDoubleDataSource(sentenceAudio));
        double[] f0Array = tracker.getF0Contour();
        if (f0Array == null) {
            return null;
        }
        for (int j = 0; j < f0Array.length; j++) {
            if (f0Array[j] == 0) {
                f0Array[j] = Double.NaN;
            }
        }
        if (f0Array.length >= 3) {
            f0Array = SignalProcUtils.medianFilter(f0Array, 5);
        }

        for (int j = 0; j < f0Array.length; j++) {
            if (f0Array[j] == 0)
                f0Array[j] = Double.NaN;
            else
                f0Array[j] = Math.log(f0Array[j]);
        }
        return f0Array;
    }

    /**
     * Get the audio data for the given sentence
     *
     * @param s
     *            s
     * @return the audio data for the sentence, in double representation
     * @throws IOException*             if there is a problem reading the audio data
     */
    private double[] getAudio(Sentence s, TimelineReader audio, UnitFileReader units) throws IOException {
        long tsSentenceStart = units.getUnit(s.getFirstUnitIndex()).startTime;
        long tsSentenceEnd = units.getUnit(s.getLastUnitIndex()).startTime + units.getUnit(s.getLastUnitIndex()).duration;
        long tsSentenceDuration = tsSentenceEnd - tsSentenceStart;
        Datagram[] sentenceData = audio.getDatagrams(tsSentenceStart, tsSentenceDuration);
        DatagramDoubleDataSource ddds = new DatagramDoubleDataSource(sentenceData);
        double[] sentenceAudio = ddds.getAllData();
        return sentenceAudio;
    }

    /**
     * For the given log f0 contour, compute an interpolation across NaN sections
     *
     * @param rawLogF0Contour
     *            rawLogF0Contour
     * @return a version of the LogF0 contour for which values are interpolated across NaN regions
     */
    private double[] getInterpolatedLogF0Contour(double[] rawLogF0Contour) {
        double[] interpol = new double[rawLogF0Contour.length];
        int iLastValid = -1;
        for (int j = 0; j < rawLogF0Contour.length; j++) {
            if (!Double.isNaN(rawLogF0Contour[j])) { // a valid value
                interpol[j] = rawLogF0Contour[j];
                if (iLastValid != j - 1) { // need to interpolate
                    double prevLogF0;
                    if (iLastValid < 0) { // we don't have a previous value, use current one
                        prevLogF0 = rawLogF0Contour[j];
                    } else {
                        prevLogF0 = rawLogF0Contour[iLastValid];
                    }
                    double delta = (rawLogF0Contour[j] - prevLogF0) / (j - iLastValid);
                    double logF0 = prevLogF0;
                    for (int k = iLastValid + 1; k < j; k++) {
                        logF0 += delta;
                        interpol[k] = logF0;
                    }
                }
                iLastValid = j;
            }
        }
        return interpol;
    }

    private List<Polynomial> fitPolynomialsToSyllables(Sentence s, double[] logF0, int polynomOrder, UnitFileReader units) {
        List<Polynomial> poly = new ArrayList<Polynomial>();
        for (Syllable syl : s) {

            Pair<Integer, Integer> syllableIndices = getSyllableIndicesInSentenceArray(s, syl, logF0.length, units);

            double[] sylLogF0 = new double[syllableIndices.getSecond() - syllableIndices.getFirst()];
            System.arraycopy(logF0, syllableIndices.getFirst(), sylLogF0, 0, sylLogF0.length);
            double[] coeffs = Polynomial.fitPolynomial(sylLogF0, polynomOrder);
            if (coeffs != null) {
                poly.add(new Polynomial(coeffs));
            } else {
                poly.add(null);
            }
        }
        return poly;
    }

    /**
     * For a syllable that is part of a sentence, determine the position of the syllable in an array representing the full
     * sentence.
     *
     * @param s
     *            the sentence
     * @param syl
     *            the syllable which must be inside the sentence
     * @param arrayLength
     *            the length of an array representing the temporal extent of the sentence
     * @return a pair of ints representing start (inclusive) and end position (exclusive) of the syllable in the array
     */
    private Pair<Integer, Integer> getSyllableIndicesInSentenceArray(Sentence s, Syllable syl, int arrayLength, UnitFileReader units) {
        long tsSentenceStart = units.getUnit(s.getFirstUnitIndex()).startTime;
        long tsSentenceEnd = units.getUnit(s.getLastUnitIndex()).startTime + units.getUnit(s.getLastUnitIndex()).duration;
        long tsSentenceDuration = tsSentenceEnd - tsSentenceStart;
        long tsSylStart = units.getUnit(syl.getFirstUnitIndex()).startTime;
        assert tsSylStart >= tsSentenceStart;
        long tsSylEnd = units.getUnit(syl.getLastUnitIndex()).startTime + units.getUnit(syl.getLastUnitIndex()).duration;
        assert tsSylEnd >= tsSylStart;
        if (tsSylEnd > tsSentenceEnd) {
            tsSylEnd = tsSentenceEnd;
        }
        // TODO: check if should be kept:  assert tsSylEnd <= tsSentenceEnd;
        // Now map time to position in logF0 array:
        double factor = (double) arrayLength / (double) tsSentenceDuration;
        int iSylStart = (int) (factor * (tsSylStart - tsSentenceStart));
        int iSylEnd = (int) (factor * (tsSylEnd - tsSentenceStart));
        return new Pair<Integer, Integer>(iSylStart, iSylEnd);
    }
}
