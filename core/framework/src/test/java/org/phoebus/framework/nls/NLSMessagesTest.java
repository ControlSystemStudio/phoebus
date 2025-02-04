/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.nls;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/** JUnit test of the {@link NLS} message initialization
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NLSMessagesTest
{
    // A 'Messages' type of class needs public static String member variables
    public static String Hello;
    public static String Bye;
    public static String HowAreYou;
    public static String MissingMessage;
    public static String AppVersion;
    private static Locale original;
    private static final String MESSAGE = "messages";

    // They are initialized from a "messages*.properties" file in the same package,
    // typically using 'static' code like this:
    //
    // static
    // {
    // NLS.initializeMessages(NLSMessagesTest.class);
    // }
    //
    // For the test, we call NLS.initializeMessages with various locates
    // in a non-static way

    @BeforeAll
    public static void saveLocale()
    {
        original = Locale.getDefault();
    }

    /** Check if the expected messages were read */
    @Test
    public void testDefaultMessages()
    {
        Locale.setDefault(Locale.US);
        NLS.initializeMessages(NLSMessagesTest.class);
        System.out.println("Messages for '" + Locale.getDefault().getLanguage() + "': " + Hello + ", " + Bye);
        assertThat(Hello, equalTo("Hello"));
        assertThat(Bye, equalTo("Bye"));
    }

    /** Check if the expected messages were read */
    @Test
    public void testGermanMessages()
    {
        Locale.setDefault(Locale.GERMANY);
        NLS.initializeMessages(NLSMessagesTest.class);
        System.out.println("Messages for '" + Locale.getDefault().getLanguage() + "': " + Hello + ", " + Bye);
        assertThat(Hello, equalTo("Moin"));
        assertThat(Bye, equalTo("Tschüss"));
    }

    /** Check if we fall back to english if a localization is incomplete */
    @Test
    public void testIncompleteLocalization()
    {
        Locale.setDefault(Locale.GERMANY);
        NLS.initializeMessages(NLSMessagesTest.class);
        System.out.println("Message missing from '" + Locale.getDefault().getLanguage() + "': " + HowAreYou);
        assertThat(HowAreYou, equalTo("How are you?"));
    }

    /** Check missing messages */
    @Test
    public void testMissingMessages()
    {
        Locale.setDefault(Locale.GERMANY);
        NLS.initializeMessages(NLSMessagesTest.class);
        System.out.println("Message missing from all localizations: " + MissingMessage);
        assertThat(MissingMessage, equalTo("<MissingMessage>"));
    }
    
    /** Check if all the messages file are synchronized with the default one */
    @Test
    public void testUpdateMessages() {
        System.out.println("**compare messages.properties files***");
        URL resource = NLSMessagesTest.class.getResource(MESSAGE + ".properties");
        List<String> difference = NLSMessagesTest.checkMessageFilesDifferences(resource.getFile());
        System.out.println("**There is " + (difference != null ? difference.size() : 0) + " difference(s) found**");
        boolean appversionFound = false;
        if (difference != null && !difference.isEmpty()) {
            for (String dif : difference) {
                System.out.println(dif);
                appversionFound = !appversionFound && dif.contains("AppVersion");
            }
        }

        assertThat("Env variable value is ignored", !appversionFound);
        assertThat("Differences between properties", difference.size() == 4);
    }

    @Test
    public void checkAllMessagesResources() {
        List<String> differences = NLSMessagesTest.checkAllMessageFilesDifferences();
        if (differences != null && !differences.isEmpty()) {
            System.out.println("WARNING ! there is " + differences.size() + " difference(s) detected in Messages_{LOCAL}.properties files");
        }
        //assertThat("All resources are synchronize ", differences.isEmpty());
    }

    @AfterAll
    public static void restoreLocale() {
        Locale.setDefault(original);
    }
    
    @Test
    public void showAccentsInFile() {
        URL resource = NLSMessagesTest.class.getResource(MESSAGE + ".properties");
        if (resource != null) {
            String filePath = resource.getFile();
            System.out.println(filePath);
            String[] split = filePath.split("/core/framework/");
            //First part is the parent folder
            String parentFolder = split != null && split.length > 0 ? split[0] : null;
            System.out.println("parentFolder=" + parentFolder);
            File parentFile = new File(parentFolder);
            String suffix = "_fr.properties";
            List<File> fileList = listMessagesFiles(parentFile, suffix);
           
            for (File file : fileList) {
                List<String> replaceUnicodeInFile = replaceUnicodeInFile(file.getAbsolutePath());
                if(replaceUnicodeInFile != null && !replaceUnicodeInFile.isEmpty()) {
                    System.out.println("");
                    System.out.println("--- " + replaceUnicodeInFile.size() + " MODIFICATION(S) SUGGESTION in " + file.getAbsolutePath() + " ---");
                    for(String newLine : replaceUnicodeInFile) {
                        System.out.println(newLine);
                    }
                    System.out.println("");
                    System.out.println("------ END OF MODIFICATION(S) ------");
                }
            }
        }
        //assertThat("All resources are synchronize ", differences.isEmpty());
    }
   
    
    /**
     * Use for unit test only
     * Check if all the existing messages_{LOCALE}.properties are synchronized on default messages.propertiesresource in the project
     * 
     * @return the list of difference between the default resources , null or empty if it is synchronized
     */
    public static List<String> checkAllMessageFilesDifferences(){
        List<String> differences = new ArrayList<>();
        URL resource = NLSMessagesTest.class.getResource(MESSAGE + ".properties");
        if (resource != null) {
            String filePath = resource.getFile();
            System.out.println(filePath);
            String[] split = filePath.split("/core/framework/");
            //First part is the parent folder
            String parentFolder = split != null && split.length > 0 ? split[0] : null;
            System.out.println("parentFolder=" + parentFolder);
            File parentFile = new File(parentFolder);
            List<File> fileList = listMessagesFiles(parentFile);
           
            for (File file : fileList) {
                List<String> diff = NLSMessagesTest.checkMessageFilesDifferences(file.getAbsolutePath());
                if (diff != null && !diff.isEmpty()) {
                    differences.addAll(diff);
                }
            }

            if(differences.isEmpty()) {
                System.out.println("All the "+ MESSAGE+ "_{LOCALE}.properties files are syncronized ");
            }
            else {
                System.out.println("**There is " + differences.size() + " difference(s) found**");
                for (String dif : differences) {
                    System.out.println(dif);
                }
            }
        }
        return differences;
    }
    
    /**
     * Use for unit test only
     * Check if the existing messages_{LOCALE}.properties are synchronized on default messages.propertiesresource
     * 
     * @param clazz Class relative to which message resources are located
     * @return the list of difference between the default ressources , null or empty if it is synchronized
     */
    private static List<String> checkMessageFilesDifferences(String resourceFile) {
        List<String> differences = new ArrayList<>();
        if (resourceFile != null) {
            try {
                File defaultFile = new File(resourceFile);
                Properties defaultBundle = new Properties();
                defaultBundle.load(new FileInputStream(defaultFile));
                File parent = defaultFile.getParentFile();
                FilenameFilter fileNameFilter = new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(MESSAGE) && name.endsWith(".properties");
                    }
                };

                File[] listFiles = parent.listFiles(fileNameFilter);
                if (listFiles != null && listFiles.length > 0) {
                    // System.out.println("Number of languages found =" + listFiles.length);
                    String fileName = null;
                    String countryCode = null;
                    String countryName = null;
                    Properties compareBundle = null;
                    Object key = null;
                    String value = null;
                    Locale locale = null;
                    Enumeration<Object> compareKeys = null;
                    Enumeration<Object> defaultKeys = null;
                    for (File tmpFile : listFiles) {
                        fileName = tmpFile.getName();
                        // Do not compare to itself
                        if (!fileName.equalsIgnoreCase(defaultFile.getName())) {
                            // Extract the country code
                            countryCode = fileName.replaceFirst(MESSAGE + "_", "");
                            countryCode = countryCode.replace(".properties", "");
                            locale = getLocaleFromCountryCode(countryCode);
                            if (locale != null) {
                                countryName = locale.getDisplayCountry();
                                compareBundle = new Properties();
                                compareBundle.load(new FileInputStream(tmpFile));
                                // Check if the key exist in the LOCAL file
                                // System.out.println("Compare " + tmpFile.getName() + " to " + defaultFile.getName());
                                defaultKeys = defaultBundle.keys();
                                while (defaultKeys.hasMoreElements()) {
                                    key = defaultKeys.nextElement();
                                    if (!compareBundle.containsKey(key)) {
                                        // Ignore env variables eg ${revision}
                                        value = String.valueOf(defaultBundle.get(key));
                                        if (!(value.startsWith("${") && value.endsWith("}"))) {
                                            differences.add("Missing " + key + " in " + countryName + " resource "
                                                    + tmpFile.getAbsolutePath());
                                        }
                                    }
                                }

                                // Check if there are some key to remove in LOCAL file
                                compareKeys = compareBundle.keys();
                                while (compareKeys.hasMoreElements()) {
                                    key = compareKeys.nextElement();
                                    if (!defaultBundle.containsKey(key)) {
                                        differences.add("Remove " + key + " in " + countryName + " resource "
                                                + tmpFile.getAbsolutePath());
                                    }
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return differences;
    }
    
    private static List<File> listMessagesFiles(File folder) {
        String suffix = MESSAGE + ".properties";
        return listMessagesFiles(folder, suffix);
    }
    
    private static List<File> listMessagesFiles(File folder, String suffix) {
        List<File> fileList = new ArrayList<>();
        //Ignore target folder from build
        if(folder != null && folder.isDirectory() 
                && !folder.getAbsolutePath().contains("\\target\\")
                && !folder.getAbsolutePath().contains("\\test\\")) {
            File[] listFiles = folder.listFiles();
            for(File file : listFiles) {
                if(file.isDirectory()) {
                    List<File> list = listMessagesFiles(file,suffix);
                    fileList.addAll(list);
                }
                else if (file.getName().toLowerCase().endsWith(suffix.toLowerCase())){
                    fileList.add(file);
                }
            }
        }
        return fileList;
    }
     
    
    /**
     * To get Locale from a countryCode
     * 
     * @param countryCode (fr , en ..."
     * @return Locale
     */
    private static Locale getLocaleFromCountryCode(String countryCode) {
        Locale localFound = null;
        if (countryCode != null && !countryCode.isEmpty()) {
            Locale[] availableLocales = Locale.getAvailableLocales();
            for (Locale locale : availableLocales) {
                if (locale.getCountry().toLowerCase().equals(countryCode.toLowerCase())) {
                    localFound = locale;
                    break;
                }
            }
        }
        return localFound;
    }
    
    /**
     * Check to accents contains in a file and show the string replacement to apply
     * @param file path
     * @return new line if there is accent
     */
    private static List<String> replaceUnicodeInFile(String file) {
        List<String> newLines = new ArrayList<>();
        final char[] charList = {'é','è','ë','ê','É','à','â','À','ï','î','ô','ö', 'ù','°','µ','€', 'ç', 'œ', '$', '’', '\''};
        String defaultFile = NLSMessagesTest.class.getResource("messages_fr.properties").getFile();
        String filePath = file != null && !file.isEmpty() ? file : defaultFile;
        File parseFile = new File(filePath);
        try (FileInputStream inps = new FileInputStream(parseFile);
             InputStreamReader isr = new InputStreamReader(inps);
             BufferedReader br = new BufferedReader(isr);) {

            String line = null;
            String newLine = null;
            String replace = null;
            String toReplace = null;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                newLine = line;
                for(char car : charList) {
                    toReplace = String.valueOf(car);
                    if(newLine.contains(toReplace) && !newLine.contains("${")) {
                        replace =  String.format("%04x", (int) car);
                        newLine = newLine.replaceAll(toReplace, "\\\\u" +replace);
                    }
                }
                if(newLine.length() != line.length()) {
                    newLines.add("line[" +lineNumber+ "]=" + newLine);
                }
                lineNumber++;
            }
        }
        catch (Exception e) {
        // TODO: handle exception
        }
        
        //If no change return empty list
        return newLines;
    }
    
    

}
