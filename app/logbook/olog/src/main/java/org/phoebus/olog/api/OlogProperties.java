package org.phoebus.olog.api;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.prefs.Preferences;

public class OlogProperties {

	private static Preferences preferences;
	private static Properties defaultProperties;
	private static Properties userCFProperties;
	private static Properties userHomeCFProperties;
	private static Properties systemCFProperties;
	
	OlogProperties(){
		
		preferences = Preferences.userNodeForPackage(OlogClient.class);

		try {
			File userCFPropertiesFile = new File(System.getProperty(
					"olog.properties", ""));
			File userHomeCFPropertiesFile = new File(System
					.getProperty("user.home")
					+ "/olog.properties");
			File systemCFPropertiesFile = null;
			if (System.getProperty("os.name").startsWith("Windows")) {
				systemCFPropertiesFile = new File("/olog.properties");
			} else if (System.getProperty("os.name").startsWith("Linux")) {
				systemCFPropertiesFile = new File(
						"/etc/olog.properties");
			} else {
				systemCFPropertiesFile = new File(
						"/etc/olog.properties");
			}

			defaultProperties = new Properties();
			try {
				defaultProperties.load(this.getClass().getResourceAsStream(
						"/config/olog.properties"));
			} catch (Exception e) {
				// Do nothing simply use an empty Properties object as default.
			}

			// Not using to new Properties(default Properties) constructor to
			// make the hierarchy clear.
			// TODO replace using constructor with default.
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
	 * default logbooks files.
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	String getPreferenceValue(String key, String defaultValue) {
		return preferences.get(key, getDefaultValue(key, defaultValue));
	}

	/**
	 * cycles through the default logbooks files and return the value for the
	 * key from the highest priority file
	 * 
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	private static String getDefaultValue(String key, String defaultValue) {
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
