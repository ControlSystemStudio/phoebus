/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * The CFProperties objects holds the properties associated with the
 * channelfinder client library initialized using the channelfinder.properties or default values.
 * 
 * The order in which these files will be read.
 * 1. properties file specified using the system property <tt>channelfinder.properties</tt>.
 * 2. channelfinder.properties file in the users home direcotory.
 * 3. channelfinder.properties file in the C:/ on windows and /etc/ on linux.
 * 4. channelfinder.properties default file packaged with the library.
 * 
 * @author shroffk
 * 
 */
class CFProperties {

	private static Properties defaultProperties;
	private static Properties userCFProperties;
	private static Properties userHomeCFProperties;
	private static Properties systemCFProperties;

	/**
	 * creates a CFProperties object which is initialized by the channelfinder.properties file.
	 * 
	 */
	public CFProperties() {

		try {
			File userCFPropertiesFile = new File(System.getProperty(
					"channelfinder.properties", ""));
			File userHomeCFPropertiesFile = new File(
					System.getProperty("user.home")
							+ "/channelfinder.properties");
			File systemCFPropertiesFile = null;
			if (System.getProperty("os.name").startsWith("Windows")) {
				systemCFPropertiesFile = new File("/channelfinder.properties");
			} else if (System.getProperty("os.name").startsWith("Linux")) {
				systemCFPropertiesFile = new File(
						"/etc/channelfinder.properties");
			} else {
				systemCFPropertiesFile = new File(
						"/etc/channelfinder.properties");
			}

			// File defaultPropertiesFile = new
			// File(this.getClass().getResource(
			// "/config/channelfinder.properties").getPath());

			defaultProperties = new Properties();
			try {
				defaultProperties.load(this.getClass().getResourceAsStream(
						"/config/channelfinder.properties"));
			} catch (Exception e) {
				// The jar has been modified and the default packaged properties
				// file has been moved.
				// Simply use the empty defaultProperties.
			}

			// Not using to new Properties(default Properties) constructor to
			// make the hierarchy clear.
			systemCFProperties = new Properties(defaultProperties);
			if (systemCFPropertiesFile.exists()) {
				systemCFProperties.load(new FileInputStream(
						systemCFPropertiesFile));
			}
			userHomeCFProperties = new Properties(systemCFProperties);
			if (userHomeCFPropertiesFile.exists()) {
				userHomeCFProperties.load(new FileInputStream(
						userHomeCFPropertiesFile));
			}
			userCFProperties = new Properties(userHomeCFProperties);
			if (userCFPropertiesFile.exists()) {
				userCFProperties
						.load(new FileInputStream(userCFPropertiesFile));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * check java preferences for the requested key - then checks the various
	 * default properties files.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String getPreferenceValue(String key, String defaultValue) {
		if (userCFProperties.containsKey(key))
			return userCFProperties.getProperty(key);
		else if (userHomeCFProperties.containsKey(key))
			return userHomeCFProperties.getProperty(key);
		else if (systemCFProperties.containsKey(key))
			return systemCFProperties.getProperty(key);
		else if (defaultProperties.containsKey(key))
			return defaultProperties.getProperty(key);
		else
			return defaultValue;
	}

}
