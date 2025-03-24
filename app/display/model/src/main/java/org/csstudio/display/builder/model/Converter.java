/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;

/**
 * 'Main' for converting *.opi or older *.bob files into the current format
 * 
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Converter {
	/**
	 * @param infile
	 *            Input file (*.opi, older *.bob)
	 * @param outfile
	 *            Output file (*.bob to write)
	 * @throws Exception
	 *             on error
	 */
	/**
	 * 
	 * @return all opi files contained in a given folder
	 */
	public static final String OUTPUT_ARG = "-output";
	public static final String OPI_EXTENSION = "opi";
	public static final String BOB_EXTENSION = "bob";
	public static final String PYTHON_EXTENSION = "python";
	public static final String PY_EXTENSION = "py";
	public static final String JAVASCRIPT_EXTENSION = "javascript";
	public static final String JS_EXTENSION = "js";
	public static final String IMPORT_CSS = "org.csstudio.opibuilder";
	public static final String IMPORT_PHOEBUS = "org.csstudio.display.builder.runtime.script";
	public static final String PHOEBUS = "phoebus_";

	/**
	 * 
	 * @param folder
	 * @return all opi files contained in a given folder
	 */

	public List<String> listOpiFiles(String folder) {
		List<String> extensionsList = new ArrayList<String>();
		extensionsList.add(OPI_EXTENSION);
		return listFiles(folder, extensionsList);
	}

	/**
	 * 
	 * @return all bob files contained in a given folder
	 */
	public List<String> listBobFiles(String folder) {
		List<String> extensionsList = new ArrayList<String>();
		extensionsList.add(BOB_EXTENSION);
		return listFiles(folder, extensionsList);
	}

	/**
	 * 
	 * @return all script files contained in a given folder
	 */
	public List<String> listScriptFiles(String folder) {
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
	public boolean isScriptFile(String fileName) {
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
	public boolean isOpiFile(String fileName) {
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
	public boolean isBobFile(String fileName) {
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
				if (fileName.toLowerCase().endsWith("." + ext)) {
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
	public List<String> listFiles(String folder, List<String> searchExtension) {
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

	private void convert(final File infile, final File outfile) throws Exception {
		traceProgression(infile, outfile); // displaying current file and it output location
		try (FileOutputStream outStream = new FileOutputStream(outfile);
			 ModelWriter writer = new ModelWriter(outStream);) 
			{
				final ModelReader reader = new ModelReader(new FileInputStream(infile));
				DisplayModel model = reader.readModel();
				writer.writeModel(model);
			} catch (Exception e) {
				throw e;
			}
	}

	/**
	 * displaying 2 files
	 * 
	 * @param infile
	 * @param outfile
	 */
	protected void traceProgression(File infile, final File outfile) {
		System.out.println("Converting: " + infile + " => " + outfile);

	}

	/**
	 * @param infile
	 *            Input file (*.opi, older *.bob)
	 * @param output_dir
	 *            Folder where to create output.bob, <code>null</code> to use folder
	 *            of input file
	 * @throws Exception
	 *             on error
	 */
	private void convert(final String input, final File output_dir) throws Exception {
		final File infile = new File(input);
		if (!infile.canRead())
			throw new Exception("Cannot read " + infile);
		File outfile;

		if (isOpiFile(input))
			outfile = new File(input.substring(0, input.length() - 4) + ".bob");
		else
			outfile = new File(input);
		if (output_dir != null)
			outfile = new File(output_dir, outfile.getName());
		if (outfile.canRead())
			throw new Exception("Output file " + outfile + " exists");

		convert(infile, outfile);
	}

	protected void launchConversion(final String[] args) {
		if (args.length == 0 || args[0].startsWith("-h")) {
			System.out.println(
					"Usage: -main org.csstudio.display.builder.model.Converter [-help] [-output /path/to/folder] <files>");
			System.out.println();
			System.out.println("Converts BOY *.opi files to Display Builder *.bob format");
			System.out.println();
			System.out.println("-output /path/to/folder   - Folder into which converted files are written");
			System.out.println("<files>                   - One or more files to convert");
			return;
		}
		final List<String> files = new ArrayList<>(List.of(args));
		final File output_dir;
		if (files.get(0).startsWith("-o")) {
			if (files.size() < 2) {
				System.err.println("Missing folder for -output /path/to/folder");
				return;
			}
			output_dir = new File(files.get(1));
			files.remove(0);
			files.remove(0);
		} else
			output_dir = null;
		for (String file : files) {
			try {
				convert(file, output_dir);
			} catch (Exception ex) {
				ex.printStackTrace();
				System.err.println("Cannot convert " + file);
				logger.log(Level.WARNING, "Cannot convert " + file, ex);
			}
		}
	}

	/**
	 * @param args
	 *            Command line arguments
	 */
	public static void main(final String[] args) {

		Converter converter = new Converter();
		converter.launchConversion(args);
	}

}
