/*******************************************************************************
 * Copyright (c) 2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.preferences;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation that marks a member variable as a preference
 * 
 *  <p>Used with {@link AnnotatedPreferences}.
 *  
 *  <p>Static fields of a class with this annotation
 *  will be set from preference keys.
 *  The package name of the class is used as the preference path.
 *  The name of the field is used as the preference key,
 *  unless a different 'name' parameter is set on the annotation.
 *  
 *  <p>Supported types:
 *  <ul>
 *  <li>int
 *  <li>double
 *  <li>boolean
 *  <li>String
 *  <li>File
 *  <li>enum     - Preference string must match one of the enum labels/names
 *  <li>int[]    - Parses comma-separated int values from preference string
 *  <li>String[] - Parses comma-separated text values from preference string
 *  </ul>
 *  
 *  <p>Fields set from preferences may be 'public' as well as 'private'.
 *  
 *  @author Kay Kasemir
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Preference
{
    /**
     * default name
     * @return empty string
     */
	String name() default "";
}
