/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.nls;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


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
    private static final String FILENAME = NLS.MESSAGE + ".properties";
    private static Locale original;

    // They are initialized from a "messages*.properties" file in the same package,
    // typically using 'static' code like this:
    //
    // static
    // {
    //     NLS.initializeMessages(NLSMessagesTest.class);
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
        List<String> difference = NLS.checkMessageFilesDifferences(NLSMessagesTest.class);
        System.out.println("**There is " + (difference != null ? difference.size() : 0) + " difference(s) found**");
        boolean appversionFound = false;
        if(difference != null && !difference.isEmpty()) {
            for(String dif : difference) {
                System.out.println(dif);
                appversionFound = !appversionFound && dif.contains("AppVersion");
            }
        }
        
        assertThat("Env variable value is ignored", !appversionFound);
        assertThat("Differences between properties", difference.size() == 3);
    }
    
    @Test
    public void checkAllMessagesResources() {
        URL resource = NLSMessagesTest.class.getResource(FILENAME);
        if (resource != null) {
            String filePath = resource.getFile();
            System.out.println(filePath);
            String[] split = filePath.split("/core/framework/");
            //First part is the parent folder
            String parentFolder = split != null && split.length > 0 ? split[0] : null;
            System.out.println("parentFolder=" + parentFolder);
            File parentFile = new File(parentFolder);
            List<File> fileList = listMessagesFiles(parentFile);
            List<String> differences = new ArrayList<>();
            for (File file : fileList) {
                List<String> diff = NLS.checkMessageFilesDifferences(file.getAbsolutePath());
                if (diff != null && !diff.isEmpty()) {
                    differences.addAll(diff);
                }
            }

            System.out.println("**There is " + differences.size() + " difference(s) found**");
            for (String dif : differences) {
                System.out.println(dif);
            }
            assertThat("All resources are synchronize ", differences.isEmpty());

        }
    }
    
    private static List<File> listMessagesFiles(File folder) {
        List<File> fileList = new ArrayList<>();
        //Ignore target folder from build
        if(folder != null && folder.isDirectory() 
                && !folder.getAbsolutePath().contains("\\target\\")
                && !folder.getAbsolutePath().contains("\\test\\")) {
            File[] listFiles = folder.listFiles();
            for(File file : listFiles) {
                if(file.isDirectory()) {
                    List<File> list = listMessagesFiles(file);
                    fileList.addAll(list);
                }
                else if (file.getName().equals(FILENAME)){
                    fileList.add(file);
                }
            }
        }
        return fileList;
    }
     

    @AfterAll
    public static void restoreLocale()
    {
        Locale.setDefault(original);
    }
}
