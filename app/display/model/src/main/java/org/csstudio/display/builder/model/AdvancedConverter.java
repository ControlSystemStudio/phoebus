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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.csstudio.display.builder.model.ModelPlugin.logger;

/**
 * Extension of Converter class to manage opi folder and several features
 * assumed in opi2bob_recursive_converter.python script
 *
 * @author Katy SAINTIN
 */

public class AdvancedConverter extends Converter {

	// Singleton design pattern
	private static AdvancedConverter instance = null;

	// different state of the conversion process
	protected enum ConvertType {
		FILELIST, NEWFILE, SUCCESS, ERROR
	};

	private IConverterListener listener = null;

	public static AdvancedConverter getInstance() {
		if (instance == null) {
			instance = new AdvancedConverter();
		}

		return instance;
	}

	protected class ConverterEvent {

		private String[] fileList = null;
		private ConvertType type = null;
		private File file = null;
		private String message = null;

		protected String getMessage() {
			return message;
		}

		private void setMessage(String errorMessage) {
			this.message = errorMessage;
		}

		protected File getFile() {
			return file;
		}

		private void setFile(File file) {
			this.file = file;
		}

		protected ConvertType getType() {
			return type;
		}

		private void setType(ConvertType type) {
			this.type = type;
		}

		protected String[] getFileList() {
			return fileList;
		}

		private void setFileList(String[] fileList) {
			this.fileList = fileList;
		}

	}

	protected interface IConverterListener {
		public void convertEvent(ConverterEvent event); // tracking events
	}

	protected void setConverterListener(IConverterListener listener) {
		this.listener = listener;
	}

	/**
	 * Generate argument for the original Converter
	 *
	 * @param args
	 * @return
	 */
	public String[] generateRecursiveArguments(String[] args) {
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
			File file = null;
			for (String fileName : args) {
				if (!fileName.startsWith("-o")) {
					file = new File(fileName);
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
	private void populateTreeStructure(File rootOutputFolder, File outputFolder, File opiRootFolder) {

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
						Path tmpPath = tmpFile.toPath();
						File copyFile;
						if (isScriptFile(fileName)
								|| (!destFile.exists() && !isOpiFile(fileName) && !isBobFile(fileName))) {
							if (isScriptFile(tmpFile.getName())) {
								// Rename script file into phoebus because of changing import
								String copyFileName = PHOEBUS + fileName;
								copyFile = new File(outputFolder, copyFileName);

							} else {
								copyFile = new File(outputFolder, fileName);

							}
							Path copyPath = copyFile.toPath();
							Files.copy(tmpPath, copyPath);

						} else if (!rootOutputFolder.getAbsolutePath().equals(outputFolder.getAbsolutePath())) {
							// Move the corresponding generated bob file in the sub folder
							// Find the corresponding bob file
							List<String> listBobFiles = listBobFiles(rootOutputFolder.getAbsolutePath());
							for (String bobFile : listBobFiles) {
								File bob = new File(bobFile);
								File tempoFile = new File(outputFolder, bob.getName());
								Path bobPath = bob.toPath();
								Path tempoPath = tempoFile.toPath();
								String simpleFileName = fileName.toLowerCase().replace(OPI_EXTENSION, "");
								if (bobFile.toLowerCase().contains(simpleFileName)) {
									Files.move(bobPath, tempoPath);
									break;
								}
							}
						}

					} catch (IOException e) {
						String errMessage = "Error to move or copy " + tmpFile.getAbsolutePath();
						errMessage = errMessage + " " + e.getMessage();
						logger.log(Level.WARNING, errMessage, e);
						notifyConvertEvent(ConvertType.ERROR, errMessage + " may already exist in the output folder");
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Tracking information about the currant converting file and get access to the
	 * case newFile
	 */
	protected void traceProgression(File infile, File outfile) {
		super.traceProgression(infile, outfile);
		notifyConvertEvent(ConvertType.NEWFILE, infile);
	}

	/**
	 * This method manage the different state of the conversion process and update
	 * the JDialog display
	 * 
	 * @param type
	 * @param value
	 */
	private void notifyConvertEvent(ConvertType type, Object value) {
		if (listener != null) {
			ConverterEvent event = new ConverterEvent();
			event.setType(type);
			boolean setValue = false;
			switch (type) {
			case FILELIST: // before conversion
				if (value instanceof String[]) {
					// get an array with every opi file to convert
					setValue = true;
					event.setFileList((String[]) value);
				}
				break;
			case NEWFILE: // every new file to convert
				if (value instanceof File) {
					setValue = true;
					event.setFile((File) value);
				}
				break;
			case SUCCESS:
			case ERROR:
			default:
				break;
			}

			if (!setValue) {
				event.setMessage(String.valueOf(value));
			}
			listener.convertEvent(event);
		}
	}

	/**
	 * Fix error conversion that Converter not manage
	 */
	private void updateScriptsAndBobFiles(File outputFolderFile) {
		// Changing import in script
		System.out.println("Update import in scripts");
		List<String> scriptList = listScriptFiles(outputFolderFile.getAbsolutePath());
		List<String> bobList = listBobFiles(outputFolderFile.getAbsolutePath());
		File tmpFile = null;
		scriptList.addAll(bobList);
		Path tmpPath;
		for (String file : scriptList) {

			try {
				if (!isScriptFile(file) || file.contains(PHOEBUS))
					tmpFile = new File(file);
				tmpPath = tmpFile.toPath();

				// Change import css to phoebus in embedded script
				String contains = Files.readString(tmpPath, Charset.defaultCharset());
				String newContains = contains.replaceAll(IMPORT_CSS, IMPORT_PHOEBUS);

				// add name of the file in phoebus
				String name = "<name>" + tmpFile.getName().substring(0, tmpFile.getName().length() - 4) + "</name>\n";

				if (isBobFile(tmpFile.getName())) {
					// regex used to find the place where the name should be (between the display
					// tag and background_color)
					String research = "<display(.+?)<background_color>";
					Pattern formula = Pattern.compile(research, Pattern.DOTALL);
					Matcher match = formula.matcher(newContains);
					match.find();
					String oldName = match.group(1);
					// if the place found before does not contain the name tag we create a name
					// based on it file name
					if (!oldName.contains("<name>")) {
						Pattern bckgrnd = Pattern.compile("<background_color>");
						Matcher index = bckgrnd.matcher(newContains);
						List<Integer> result = new ArrayList<>();
						while (index.find()) {
							result.add(index.start());
						}
						for (int i = 0; i < newContains.length(); i++) {
							if (i == result.get(0) - 1) {
								newContains = newContains.substring(0, i - 1) + name + newContains.substring(i);
							}
						}
					}

					// convert & to &&
					// regex used to find the place where the boolean expression should be (between
					// the <exp tag and >)
					String search = "<exp[^>]*>";
					Pattern pattern = Pattern.compile(search);
					Matcher matcher = pattern.matcher(newContains);
					StringBuffer next = new StringBuffer();
					while (matcher.find()) {
						String hit = matcher.group();
						if (!hit.contains("&amp;&amp;")) {
							String modifAnd = hit.replace("&amp;", "&amp;&amp;");
							matcher.appendReplacement(next, Matcher.quoteReplacement(modifAnd));
						}

					}
					matcher.appendTail(next);
					newContains = next.toString();

					// convert | to ||
					Pattern pattern2 = Pattern.compile(search);
					Matcher matcher2 = pattern2.matcher(newContains);
					StringBuffer next2 = new StringBuffer();
					while (matcher2.find()) {
						String hit = matcher2.group();
						if (!hit.contains("||")) {
							String modifOr = hit.replace("|", "||");
							matcher2.appendReplacement(next2, Matcher.quoteReplacement(modifOr));
						}

					}
					matcher2.appendTail(next2);
					newContains = next2.toString();
				}

				// Replace embedded opi by bob
				newContains = newContains.replaceAll(OPI, BOB); // prevent replaceAll problem for opi becoming .bob in
																// the bob file
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
				Files.writeString(tmpPath, newContains, Charset.defaultCharset());
			} catch (Exception e) {
				e.printStackTrace();
				String errMessage = "Error update " + file;
				errMessage = errMessage + " " + e.getMessage();
				//System.err.println(errMessage);
				logger.log(Level.WARNING, errMessage, e);
				notifyConvertEvent(ConvertType.ERROR, errMessage);
			}
		}

	}

	@Override
	protected void launchConversion(String[] args) {
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
					logger.log(Level.WARNING, errMessage, e);
					notifyConvertEvent(ConvertType.ERROR, errMessage);
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
		notifyConvertEvent(ConvertType.FILELIST, generateArguments);

		super.launchConversion(generateArguments);
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
		notifyConvertEvent(ConvertType.SUCCESS, "Process is finished");

	}

	/**
	 * Call Converter main class with full arguments list all the opi file
	 *
	 * @param i
	 */
	public static void main(String[] args) {
		AdvancedConverter.getInstance().launchConversion(args);
	}
}
