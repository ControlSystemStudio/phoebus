/*******************************************************************************
 * Copyright (c) 2015-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * Extension of Converter class to manage opi folder and several features
 * assumed in opi2bob_recursive_converter.python script
 * 
 * @author Katy SAINTIN
 */

public class AdvancedConverter extends Converter {
	public static final String OUTPUT_ARG = "-output";
	public static final String OPI_EXTENSION = ".opi";
	public static final String BOB_EXTENSION = ".bob";
	public static final String PYTHON_EXTENSION = ".python";
	public static final String PY_EXTENSION = ".py";
	public static final String JAVASCRIPT_EXTENSION = ".javascript";
	public static final String JS_EXTENSION = ".js";
	public static final String IMPORT_CSS = "org.csstudio.opibuilder";
	public static final String IMPORT_PHOEBUS = "org.csstudio.display.builder.runtime.script";
	public static final String PHOEBUS = "phoebus_";

	/**
	 * 
	 * @return all opi files contained in a given folder
	 */
	public static List<String> listOpiFiles(String folder) {
		List<String> extensionsList = new ArrayList<String>();
		extensionsList.add(OPI_EXTENSION);
		return listFiles(folder, extensionsList);
	}

	/**
	 * 
	 * @return all bob files contained in a given folder
	 */
	public static List<String> listBobFiles(String folder) {
		List<String> extensionsList = new ArrayList<String>();
		extensionsList.add(BOB_EXTENSION);
		return listFiles(folder, extensionsList);
	}

	/**
	 * 
	 * @return all script files contained in a given folder
	 */
	public static List<String> listScriptFiles(String folder) {
		List<String> extensionsList = new ArrayList<String>();
		extensionsList.add(PYTHON_EXTENSION);
		extensionsList.add(PY_EXTENSION);
		extensionsList.add(JAVASCRIPT_EXTENSION);
		extensionsList.add(JS_EXTENSION);
		return listFiles(folder, extensionsList);
	}

	/**
	 * Return true if it is a script
	 * 
	 * @param fileName
	 * @return true if the file is a script file
	 */
	public static boolean isScriptFile(String fileName) {
		List<String> extensionsList = new ArrayList<String>();
		extensionsList.add(PYTHON_EXTENSION);
		extensionsList.add(PY_EXTENSION);
		extensionsList.add(JAVASCRIPT_EXTENSION);
		extensionsList.add(JS_EXTENSION);
		return matchExtensions(fileName, extensionsList);
	}

	/**
	 * Return true if it is a opi file
	 * 
	 * @param fileName
	 * @return true if the file is a opi file
	 */
	public static boolean isOpiFile(String fileName) {
		List<String> extensionsList = new ArrayList<String>();
		extensionsList.add(OPI_EXTENSION);
		return matchExtensions(fileName, extensionsList);
	}
	
	/**
	 * Return true if it is a bob file
	 * 
	 * @param fileName
	 * @return true if the file is a bob file
	 */
	public static boolean isBobFile(String fileName) {
		List<String> extensionsList = new ArrayList<String>();
		extensionsList.add(BOB_EXTENSION);
		return matchExtensions(fileName, extensionsList);
	}

	/**
	 * 
	 * @param fileName
	 * @param extensionsList
	 * @return true if the file is matched with the given extensions
	 */
	public static boolean matchExtensions(String fileName, List<String> extensionsList) {
		boolean match = false;
		if (fileName != null && extensionsList != null && !extensionsList.isEmpty()) {
			for (String ext : extensionsList) {
				if (fileName.toLowerCase().endsWith(ext)) {
					match = true;
					break;
				}
			}
		}
		return match;
	}

	/**
	 * 
	 * @return all files contained in a given folder and match with given extension
	 */
	public static List<String> listFiles(String folder, List<String> searchExtension) {
		List<String> searchFiles = new ArrayList<String>();
		File folderFile = new File(folder);
		if (folderFile.exists() && folderFile.isDirectory()) {
			File[] listFiles = folderFile.listFiles();
			String filePath = null;
			for (File file : listFiles) {
				filePath = file.getAbsolutePath();
				if (file.isDirectory()) {
					List<String> tmpFiles = listFiles(filePath, searchExtension);
					if (tmpFiles != null && !tmpFiles.isEmpty()) {
						searchFiles.addAll(tmpFiles);
					}
				} else if (matchExtensions(filePath, searchExtension)) {
					searchFiles.add(filePath);
				}
			}
		}
		return searchFiles;
	}

	/**
	 * Generate argument for the original Converter
	 * 
	 * @param args
	 * @return
	 */
	public static String[] generateRecursiveArguments(String[] args) {
		List<String> converterArguments = new ArrayList<>();
		if (args != null && args.length > 0) {
			String firstArg = args[0];
			File outputFolderFile = null;
			if (firstArg.startsWith("-o") && args.length > 1) {
				outputFolderFile = new File(args[1]);
			} else {
				// output folder is not defined -output /path/to/folder
				// Use the first argument to build the output folder
				File firstFile = new File(firstArg);
				if (firstFile.exists()) {
					if (firstFile.isDirectory()) {
						outputFolderFile = new File(firstFile.getAbsolutePath());
					} else {
						outputFolderFile = new File(firstFile.getParentFile().getAbsolutePath());
					}
				}
			}

			// Defined output folder
			if (outputFolderFile != null) {
				converterArguments.add(OUTPUT_ARG);
				converterArguments.add(outputFolderFile.getAbsolutePath());
			}

			// Assume a folder for a recursive management
			for (String fileName : args) {
				if (!fileName.startsWith("-o")) {
					File file = new File(fileName);
					if (file.exists()) {
						if (file.isDirectory()) {
							List<String> listOpiFiles = listOpiFiles(file.getAbsolutePath());
							if (listOpiFiles != null && !listOpiFiles.isEmpty()) {
								converterArguments.addAll(listOpiFiles);
							}
						} else if (fileName.toLowerCase().endsWith(OPI_EXTENSION)) {
							converterArguments.add(fileName);
						}
					}
				}
			}

		}

		return (String[]) converterArguments.toArray(new String[converterArguments.size()]);
	}

	/**
	 * This method generate the tree structure hosts bob script and pictures ...
	 */
	private static void populateTreeStructure(File rootOutputFolder, File outputFolder, File opiRootFolder) {

		if (outputFolder != null && opiRootFolder != null) {
			// Create the tree structure in root folder then copy all files except opi file
			File[] listFiles = opiRootFolder.listFiles();

			for (File tmpFile : listFiles) {
				if (tmpFile.isDirectory() && !outputFolder.getName().equals(tmpFile.getName())) {
					// Create folder in the output parent folder
					File subFolder = new File(outputFolder, tmpFile.getName());
					populateTreeStructure(rootOutputFolder, subFolder, tmpFile);
				} else if (tmpFile.isFile()) {
					try {
						if (!outputFolder.exists() && !outputFolder.getName().equals(rootOutputFolder.getName())) {
							outputFolder.mkdir();
						}
						// Copy all other file in the output folder if not exist
						String fileName = tmpFile.getName();
						File destFile = new File(outputFolder, fileName);
						if (isScriptFile(fileName)||(!destFile.exists() && !isOpiFile(fileName) && !isBobFile(fileName)) ) {
							if (isScriptFile(tmpFile.getName())) {
								// Rename script file into phoebus because of changing import
								String copyFileName = PHOEBUS + fileName;
								File copyFile = new File(outputFolder, copyFileName);
								FileUtils.copyFile(tmpFile, copyFile);
							} else {
								FileUtils.copyFileToDirectory(tmpFile, outputFolder, true);
							}
						} else if (!rootOutputFolder.getAbsolutePath().equals(outputFolder.getAbsolutePath())) {
							// Move the corresponding generated bob file in the sub folder
							// Find the corresponding bob file
							List<String> listBobFiles = listBobFiles(rootOutputFolder.getAbsolutePath());
							for (String bobFile : listBobFiles) {
								String simpleFileName = fileName.toLowerCase().replace(OPI_EXTENSION, "");
								if (bobFile.toLowerCase().contains(simpleFileName)) {
									FileUtils.moveToDirectory(new File(bobFile), outputFolder, true);
									break;
								}
							}
						}

					} catch (IOException e) {
						String errMessage = "Error to move or copy " + tmpFile.getAbsolutePath();
						errMessage = errMessage + " " + e.getMessage();
						System.err.println(errMessage);
						// e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Fix error conversion that Converter not manage
	 */
	private static void updateScriptsAndBobFiles(File outputFolderFile) {
		// Changing import in script
		System.out.println("Update import in scripts");
		List<String> scriptList = listScriptFiles(outputFolderFile.getAbsolutePath());
		List<String> bobList = listBobFiles(outputFolderFile.getAbsolutePath());
		scriptList.addAll(bobList);
		File tmpFile = null;
		for (String file : scriptList) {
			try {
				if (!isScriptFile(file) || file.contains(PHOEBUS))
					tmpFile = new File(file);
				// Change import css to phoebus in embedded script
				String contains = FileUtils.readFileToString(tmpFile, Charset.defaultCharset());
				String newContains = contains.replaceAll(IMPORT_CSS, IMPORT_PHOEBUS);

				// Replace embedded opi by bob
				newContains = newContains.replaceAll(OPI_EXTENSION, BOB_EXTENSION);
				newContains = newContains.replaceAll(OPI_EXTENSION.toUpperCase(), BOB_EXTENSION.toUpperCase());

				// Replace name of script for phoebus => phoebus_scritfile.py
				for (String scriptFile : scriptList) {
					if (isScriptFile(scriptFile)) {
						String scriptFileName = new File(scriptFile).getName();
						scriptFileName = scriptFileName.replaceFirst(PHOEBUS, "");
						newContains = newContains.replaceAll(scriptFileName, PHOEBUS + scriptFileName);
					}
				}
				// Write new contains
				FileUtils.writeStringToFile(tmpFile, newContains, Charset.defaultCharset());
			} catch (Exception e) {
				String errMessage = "Error update " + file;
				errMessage = errMessage + " " + e.getMessage();
				System.err.println(errMessage);
			}
		}

	}

	/**
	 * Call Converter main class with full arguments list all the opi file
	 * 
	 * @param i
	 */
	public static void main(String[] args) {

		if (args.length == 0 || args[0].startsWith("-h")) {
			System.out.println(
					"Usage: -main org.csstudio.display.builder.model.AdvancedConverter [-help] [-output /path/to/folder] </path/to/opi/folder>");
			System.out.println();
			System.out.println("Converts BOY *.opi files to Display Builder *.bob format");
			System.out.println();
			System.out.println(
					"-output /path/to/folder   - Folder into which converted files are written, /path/to/opi/folder if not defined");
			System.out.println(
					"/path/to/opi/folder       - The folder that contains opi files, all the opi files will be processed recursively ");
			return;
		}

		// Parse arguments in order to generate a full arguments with all opi file
		String[] generateArguments = generateRecursiveArguments(args);

		// Find opi root folder the shorter path parent folder
		File opiRootFolder = null;
		File outputFolderFile = null;
		// Create output folder if not exist
		if (generateArguments != null && generateArguments.length > 1) {
			outputFolderFile = new File(generateArguments[1]);
			if (!outputFolderFile.exists()) {
				try {
					outputFolderFile.mkdir();
				} catch (Exception e) {
					String errMessage = "Error on Output folder " + outputFolderFile.getAbsolutePath() + " creation";
					errMessage = errMessage + " " + e.getMessage();
					System.err.println(errMessage);
					// e.printStackTrace();
				}
			}

			// Find opi root folder the shorter path parent folder
			File parentFile = null;
			File opiFile = null;

			for (int i = 2; i < generateArguments.length; i++) {
				opiFile = new File(generateArguments[i]);
				parentFile = opiFile.getParentFile();
				if (opiRootFolder == null) {
					opiRootFolder = parentFile;
				}

				if (parentFile.getAbsolutePath().length() < opiRootFolder.getAbsolutePath().length()) {
					opiRootFolder = parentFile;
				}
			}
		}
		// Call main super class
		Converter.main(generateArguments);
		System.out.println("Conversion done");

		// All the bob are generated in the output folder
		// Generate the same tree structure from opi project
		// Copy and move scripts and pictures in the generated output folder
		System.out.println("populate output tree structure");
		populateTreeStructure(outputFolderFile, outputFolderFile, opiRootFolder);

		// Pre processing modifications
		// Replace import in embedded scripts and scripts file
		System.out.println("pre processing modifications");
		updateScriptsAndBobFiles(outputFolderFile);

	}
}
