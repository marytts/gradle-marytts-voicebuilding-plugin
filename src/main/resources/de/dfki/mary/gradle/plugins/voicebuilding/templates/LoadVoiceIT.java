package marytts.voice.${voice.nameCamelCase};

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import javax.sound.sampled.AudioInputStream;
import javax.xml.parsers.ParserConfigurationException;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.datatypes.MaryDataType;
import marytts.modules.synthesis.Voice;
import marytts.${voice.type == 'hsmm' ? 'htsengine.HMMVoice' : 'unitselection.UnitSelectionVoice'};
import marytts.util.MaryRuntimeUtils;
import marytts.util.dom.DomUtils;

import org.apache.commons.io.IOUtils;

import org.junit.BeforeClass;
import org.junit.Test;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class LoadVoiceIT {

	public static String exampleText;

	@BeforeClass
	public static void beforeClass() throws Exception {
		MaryRuntimeUtils.ensureMaryStarted();
		InputStream exampleStream = LoadVoiceIT.class.getResourceAsStream("TEXT.${voice.maryLocale}.example");
		exampleText = IOUtils.toString(exampleStream, "UTF-8");
	}

	@Test
	public void canLoadVoice() throws Exception {
		Config config = new Config();
		Voice voice = new ${voice.type == 'hsmm' ? 'HMM' : 'UnitSelection'}Voice(config.getName(), null);
		assertNotNull(voice);
	}

	@Test
	public void canSetVoice() throws Exception {
		MaryInterface mary = new LocalMaryInterface();
		String voiceName = new Config().getName();
		mary.setVoice(voiceName);
		assertEquals(voiceName, mary.getVoice());
	}

	@Test
	public void canProcessTextToSpeech() throws Exception {
		MaryInterface mary = new LocalMaryInterface();
		mary.setVoice(new Config().getName());
		AudioInputStream audio = mary.generateAudio(exampleText);
		assertNotNull(audio);
	}

	@Test
	public void canProcessToTargetfeatures() throws Exception {
		MaryInterface mary = new LocalMaryInterface();
		Locale locale = new Locale(${voice.maryLocale.split('_').collect{ "\"$it\"" }.join(', ')});
		mary.setLocale(locale);
		mary.setOutputType(MaryDataType.TARGETFEATURES.name());
		String out = mary.generateText(exampleText);
		assertNotNull(out);
	}

	@Test
	public void canProcessTokensToTargetfeatures() throws Exception {
		MaryInterface mary = new LocalMaryInterface();
		Locale locale = new Locale(${voice.maryLocale.split('_').collect{ "\"$it\"" }.join(', ')});
		mary.setLocale(locale);
		mary.setInputType(MaryDataType.TOKENS.name());
		mary.setOutputType(MaryDataType.TARGETFEATURES.name());
		Document doc = getExampleTokens(mary.getLocale());
		String out = mary.generateText(doc);
		assertNotNull(out);
	}

	@Test
	public void canProcessTokensToSpeech() throws Exception {
		MaryInterface mary = new LocalMaryInterface();
		Locale locale = new Locale(${voice.maryLocale.split('_').collect{ "\"$it\"" }.join(', ')});
		mary.setLocale(locale);
		mary.setInputType(MaryDataType.TOKENS.name());
		Document doc = getExampleTokens(mary.getLocale());
		AudioInputStream audio = mary.generateAudio(doc);
		assertNotNull(audio);
	}

	private Document getExampleTokens(Locale locale) throws ParserConfigurationException, SAXException, IOException {
		String example = MaryDataType.getExampleText(MaryDataType.TOKENS, locale);
		assertNotNull(example);
		Document doc = DomUtils.parseDocument(example);
		return doc;
	}

}
