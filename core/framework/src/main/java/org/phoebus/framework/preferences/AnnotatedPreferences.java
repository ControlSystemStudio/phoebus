/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.preferences;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Helper for setting fields of an annotated 'preference' class
 *  
 *  @author Kay Kasemir
 */
public class AnnotatedPreferences
{
	/** Initialize static fields of a class from preferences.
	 * 
	 *  <p>Fields to be set from preferences need to be marked
	 *  as @{@link Preference}
	 *  
	 *  @param clazz Class to be initialized, also sets the package name for preference keys
	 *  @param preferences_properties_filename Preference properties file, loaded as resource of the `clazz`
	 *  
	 *  @return {@link PreferencesReader} in case caller wants to directly read some items for special handling
	 */
	public static PreferencesReader initialize(final Class<?> clazz, final String preferences_properties_filename)
	{
		final PreferencesReader prefs = new PreferencesReader(clazz, preferences_properties_filename);

		for (Field field : clazz.getDeclaredFields())
		{
			try
			{
				// Look for fields annotated as @Preference
				final Preference anno = field.getAnnotation(Preference.class);
				if (anno == null)
					continue;
				
				// Non-default preference name? Otherwise default to field name
				final String pref_name = anno.name().isBlank()
						               ? field.getName()
						               : anno.name();

			    // Does non-public field need to be made accessible?
			    final boolean make_accessible = (field.getModifiers() & Modifier.PUBLIC) == 0;
			    if (make_accessible)
		        	field.setAccessible(true);
			    
			    // Assign preference to the various field types
				try
				{
					if (field.getType() == int.class)
						field.setInt(clazz, prefs.getInt(pref_name));
					else if (field.getType() == long.class)
						field.setLong(clazz, prefs.getLong(pref_name));
					else if (field.getType() == String.class)
						field.set(clazz, prefs.get(pref_name));
					else if (field.getType() == double.class)
						field.setDouble(clazz, prefs.getDouble(pref_name));
					else if (field.getType() == boolean.class)
						field.setBoolean(clazz, prefs.getBoolean(pref_name));
					else if (field.getType() == int[].class)
					{
						final String[] items = prefs.get(pref_name).split("\\s*,\\s*");
				        if (items.length == 0  ||  (items.length == 1 && items[0].isEmpty()))
				        	throw new Exception("Expect at least one integer");
						final int nums[] = new int[items.length];
						for (int i=0; i<nums.length; ++i)
							nums[i] = Integer.parseInt(items[i]);
						field.set(clazz, nums);

					}
					else if (field.getType() == String[].class)
					{
						String[] items = prefs.get(pref_name).split("\\s*,\\s*");
				        // split() will turn "" into { "" }, which we change into empty array
				        if (items.length == 0  ||  (items.length == 1 && items[0].isEmpty()))
				        	items = new String[0];

						field.set(clazz, items);
					}
					else if (field.getType() == File.class)
						field.set(clazz, new File(prefs.get(pref_name)));
					else if (field.getType().isEnum())
					{
						// Find matching enum option
						final String value = prefs.get(pref_name);
						for (Object option : field.getType().getEnumConstants())
						{
							final String name = ((Enum<?>) option).name();
							if (name.equals(value))
							{
								field.set(clazz, option);
								return prefs;
							}
						}
						throw new Exception("Cannot determine enum option for value '" + value + "'");
					}
					else
						throw new Exception("Cannot handle fields of type " + field.getType());
				}
				finally
				{
					if (make_accessible)
			        	field.setAccessible(false);
				}
			}
			catch (Exception ex)
			{
				Logger.getLogger(AnnotatedPreferences.class.getPackageName())
				      .log(Level.WARNING,
				    	   "Failed to assign value to annotated preference " + clazz.getName() + " field " + field.getName(),
				    	   ex);
			}
		}
		
		return prefs;
	}

}
