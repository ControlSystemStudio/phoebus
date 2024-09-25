package org.csstudio.display.builder.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit test for AdvancedConverter class
 */
public class AdvancedConverterTest {

	/**
	 * Test for listOpiFiles from a folder
	 */
	@Test
	public void listOpiFilesTest() {
		String tmpOpiFolder = "src/test/resources/opiFiles";
		File file = new File(tmpOpiFolder);

		List<String> listOpiFiles = AdvancedConverter.getInstance().listOpiFiles(file.getAbsolutePath());
		assertTrue(listOpiFiles.size() == 4);

		for (String opiFile : listOpiFiles) {
			assertTrue(opiFile.toLowerCase().endsWith(AdvancedConverter.OPI_EXTENSION));
		}
	}
	
	@Test
	public void listScriptFilesTest() {
		String tmpOpiFolder = "src/test/resources/opiFiles";
		File file = new File(tmpOpiFolder);

		List<String> listScriptFiles = AdvancedConverter.getInstance().listScriptFiles(file.getAbsolutePath());
		assertTrue(listScriptFiles.size() == 4);

		for (String scriptFile : listScriptFiles) {
			assertTrue(AdvancedConverter.getInstance().isScriptFile(scriptFile));
		}
	}
	

	@Test
	public void generateRecursiveArgumentsTest() {
		try {
			String tmpOpiFolder = "src/test/resources/opiFiles";
			File file = new File(tmpOpiFolder);
			String[] arg = new String[] { file.getAbsolutePath() };
			String[] generateArguments = AdvancedConverter.getInstance().generateRecursiveArguments(arg);
//			System.out.println(Arrays.toString(generateArguments));
//			System.out.println(generateArguments.length);
			assertTrue(generateArguments.length == 6);
			assertTrue(generateArguments[0].equals("-output"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
