/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui;

import java.io.File;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.authorization.AuthorizationService;
import org.phoebus.applications.alarm.ui.authorization.FileBasedAuthorizationService;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/** Common alarm UI helpers
 *
 *  <p>Icons for {@link SeverityLevel}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmUI
{
    // Next arrays follow the ordinal of SeverityLevel
    private static final Color[] severity_colors = new Color[]
    {
        Color.rgb(  0, 100,   0), // OK
        Color.rgb(120,  90,  10), // MINOR_ACK
        Color.rgb(100,   0,   0), // MAJOR_ACK
        Color.rgb(100,  50, 100), // INVALID_ACK
        Color.rgb(100,  50, 100), // UNDEFINED_ACK
        Color.rgb(207, 192,   0), // MINOR
        Color.rgb(255,   0,   0), // MAJOR
        Color.rgb(255,   0, 255), // INVALID
        Color.rgb(255,   0, 255), // UNDEFINED
    };

    private static final Image[] severity_icons = new Image[]
    {
        null, // OK
        ImageCache.getImage(AlarmUI.class, "/icons/minor_ack.png"),
        ImageCache.getImage(AlarmUI.class, "/icons/major_ack.png"),
        ImageCache.getImage(AlarmUI.class, "/icons/undefined_ack.png"),
        ImageCache.getImage(AlarmUI.class, "/icons/undefined_ack.png"),
        ImageCache.getImage(AlarmUI.class, "/icons/minor.png"),
        ImageCache.getImage(AlarmUI.class, "/icons/major.png"),
        ImageCache.getImage(AlarmUI.class, "/icons/undefined.png"),
        ImageCache.getImage(AlarmUI.class, "/icons/undefined.png")
    };

    public static final Image disabled_icon = ImageCache.getImage(AlarmUI.class, "/icons/disabled.png");
    
    private static boolean initialized = false;
    private static AuthorizationService authorizationService;
    
    /** @param severity {@link SeverityLevel}
     *  @return Color
     */
    public static Color getColor(final SeverityLevel severity)
    {
        return severity_colors[severity.ordinal()];

    }

    /** @param severity {@link SeverityLevel}
     *  @return Icon, may be <code>null</code>
     */
    public static Image getIcon(final SeverityLevel severity)
    {
        return severity_icons[severity.ordinal()];
    }

    public static boolean mayAcknowledge()
    {
        if (initialized)
        {
            return authorizationService.hasAuthorization("alarm_ack");
        } 
        return false;
    }

    public static boolean mayConfigure()
    {
        if (initialized)
        {
            return authorizationService.hasAuthorization("alarm_config");
        } 
        return false;
    }

    public static void initializeAuthorizationService()
    { 
        if (! initialized)
        {
            final PreferencesReader prefs = new PreferencesReader(AlarmSystem.class, "/alarm_preferences.properties");
            File auth_conf = new File(prefs.get("authorization_file"));
            authorizationService = new FileBasedAuthorizationService(auth_conf);
            initialized = true;
        }
    }
}
