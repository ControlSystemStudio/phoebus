/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;

/** Widget property with Filename as value.
 *
 *  <p>Holds either a URL, path to local file or - if running under RCP -
 *  a workspace location.
 *  These are all stored as a string, and this class is mostly a marker
 *  for the editor.
 *  Editor can then offer a file browser in addition to allowing
 *  plain text entry.
 *
 *  @author Kay Kasemir
 */
public class FilenameWidgetProperty extends StringWidgetProperty
{
    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public FilenameWidgetProperty(
            final WidgetPropertyDescriptor<String> descriptor,
            final Widget widget,
            final String default_value)
    {
        super(descriptor, widget, default_value);
    }
}
