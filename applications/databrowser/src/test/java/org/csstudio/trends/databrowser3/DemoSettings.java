/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

/** Site-specific settings for demos
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DemoSettings
{
    // For now you need to update these when trying to execute the demo code.
    // Could read these from environment variables, local file..
    public static final String url = "jdbc:mysql://ics-web.sns.ornl.gov/archive";
    public static final String name_pattern = "*B*";
}