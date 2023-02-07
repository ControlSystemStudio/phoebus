/*******************************************************************************
 * Copyright (c) 2018-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui;

import java.util.Arrays;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.security.authorization.AuthorizationService;
import org.phoebus.ui.Preferences;
import org.phoebus.ui.javafx.ImageCache;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

/** Common alarm UI helpers
 *
 *  <p>Icons for {@link SeverityLevel}.
 *
 *  @author Tanvi Ashwarya
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmUI
{
    /** Factor used to adjust color brightness or saturation */
    private static final double ADJUST = 0.5;

    // Next arrays follow the ordinal of SeverityLevel
    private static final Color[] severity_colors = new Color[]
    {
        createColor(Preferences.ok_severity_text_color),                                         // OK
        createColor(Preferences.minor_severity_text_color)    .deriveColor(0, 1.0, ADJUST, 1.0), // MINOR_ACK
        createColor(Preferences.major_severity_text_color)    .deriveColor(0, 1.0, ADJUST, 1.0), // MAJOR_ACK
        createColor(Preferences.invalid_severity_text_color)  .deriveColor(0, 1.0, ADJUST, 1.0), // INVALID_ACK
        createColor(Preferences.undefined_severity_text_color).deriveColor(0, 1.0, ADJUST, 1.0), // UNDEFINED_ACK
        createColor(Preferences.minor_severity_text_color),                                      // MINOR
        createColor(Preferences.major_severity_text_color),                                      // MAJOR
        createColor(Preferences.invalid_severity_text_color),                                    // INVALID
        createColor(Preferences.undefined_severity_text_color)                                   // UNDEFINED
    };

    private static Color createColor(int[] rgb)
    {
        if (rgb.length == 3)
            return Color.rgb(rgb[0], rgb[1], rgb[2]);
        else if (rgb.length == 4)
            return Color.rgb(rgb[0], rgb[1], rgb[2], rgb[3]/255.0);
        throw new IllegalStateException("Expecting R,G,B or R,G,B,A, got " + Arrays.toString(rgb));
    }

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

    private static final Background[] severity_backgrounds = new Background[]
    {
        new Background(new BackgroundFill(createColor(Preferences.ok_severity_background_color), CornerRadii.EMPTY, Insets.EMPTY)), // OK
        new Background(new BackgroundFill(createColor(Preferences.minor_severity_background_color)    .deriveColor(0, ADJUST, 1.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY)), // MINOR_ACK
        new Background(new BackgroundFill(createColor(Preferences.major_severity_background_color)    .deriveColor(0, ADJUST, 1.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY)), // MAJOR_ACK
        new Background(new BackgroundFill(createColor(Preferences.invalid_severity_background_color)  .deriveColor(0, ADJUST, 1.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY)), // INVALID_ACK
        new Background(new BackgroundFill(createColor(Preferences.undefined_severity_background_color).deriveColor(0, ADJUST, 1.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY)), // UNDEFINED_ACK
        new Background(new BackgroundFill(createColor(Preferences.minor_severity_background_color),                                      CornerRadii.EMPTY, Insets.EMPTY)), // MINOR
        new Background(new BackgroundFill(createColor(Preferences.major_severity_background_color),                                      CornerRadii.EMPTY, Insets.EMPTY)), // MAJOR
        new Background(new BackgroundFill(createColor(Preferences.invalid_severity_background_color),                                    CornerRadii.EMPTY, Insets.EMPTY)), // INVALID
        new Background(new BackgroundFill(createColor(Preferences.undefined_severity_background_color),                                  CornerRadii.EMPTY, Insets.EMPTY)), // UNDEFINED
    };

    private static final Background[] legacy_table_severity_backgrounds = new Background[]
    {
        new Background(new BackgroundFill(createColor(Preferences.ok_severity_text_color), CornerRadii.EMPTY, Insets.EMPTY)), // OK
        new Background(new BackgroundFill(createColor(Preferences.minor_severity_text_color)    .deriveColor(0, ADJUST, 1.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY)), // MINOR_ACK
        new Background(new BackgroundFill(createColor(Preferences.major_severity_text_color)    .deriveColor(0, ADJUST, 1.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY)), // MAJOR_ACK
        new Background(new BackgroundFill(createColor(Preferences.invalid_severity_text_color)  .deriveColor(0, ADJUST, 1.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY)), // INVALID_ACK
        new Background(new BackgroundFill(createColor(Preferences.undefined_severity_text_color).deriveColor(0, ADJUST, 1.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY)), // UNDEFINED_ACK
        new Background(new BackgroundFill(createColor(Preferences.minor_severity_text_color),                                      CornerRadii.EMPTY, Insets.EMPTY)), // MINOR
        new Background(new BackgroundFill(createColor(Preferences.major_severity_text_color),                                      CornerRadii.EMPTY, Insets.EMPTY)), // MAJOR
        new Background(new BackgroundFill(createColor(Preferences.invalid_severity_text_color),                                    CornerRadii.EMPTY, Insets.EMPTY)), // INVALID
        new Background(new BackgroundFill(createColor(Preferences.undefined_severity_text_color),                                  CornerRadii.EMPTY, Insets.EMPTY)), // UNDEFINED
    };


    /** Icon for disabled alarms */
    public static final Image disabled_icon = ImageCache.getImage(AlarmUI.class, "/icons/disabled.png");

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

    /** @param severity {@link SeverityLevel}
     *  @return Background, may be <code>null</code>
     */
    public static Background getBackground(final SeverityLevel severity)
    {
        return severity_backgrounds[severity.ordinal()];
    }

    /** @param severity {@link SeverityLevel}
     *  @return Background, may be <code>null</code>
     */
    public static Background getLegacyTableBackground(final SeverityLevel severity)
    {
        return legacy_table_severity_backgrounds[severity.ordinal()];
    }

    /** Verify authorization, qualified by model's current config
     *  @param model Alarm client model
     *  @param auto Authorization name
     *  @return <code>true</code> if the user has authorization
     */
    private static boolean haveQualifiedAuthorization(final AlarmClient model, final String authorization)
    {
        if (model != null)
        {   // Check for authorization specific to this alarm model
            final String qualified = authorization + "." + model.getRoot().getName();
            if (AuthorizationService.isAuthorizationDefined(qualified))
                return AuthorizationService.hasAuthorization(qualified);
        }
        return AuthorizationService.hasAuthorization(authorization);
    }

    /** Verify acknowledge action through authorization service.
     *  @param model Alarm client model
     *  @return <code>true</code> if the user has authorization to acknowledge.
     */
    public static boolean mayAcknowledge(final AlarmClient model)
    {
        return haveQualifiedAuthorization(model, "alarm_ack");
    }

    /** Verify configure action through authorization service.
     *  @param model Alarm client model
     *  @return <code>true</code> if the user has authorization to configure.
     */
    public static boolean mayConfigure(final AlarmClient model)
    {
        return haveQualifiedAuthorization(model, "alarm_config");
    }

    /** Verify modify mode action through authorization service.
     *  @param model Alarm client model
     *  @return <code>true</code> if the user has authorization to modify maintenance/normal mode.
     */
    public static boolean mayModifyMode(final AlarmClient model)
    {
        return haveQualifiedAuthorization(model, "alarm_mode");
    }

    /** Verify disable_notify action through authorization service.
     *  @param model Alarm client model
     *  @return <code>true</code> if the user has authorization to disable notifications.
     */
    public static boolean mayDisableNotify(final AlarmClient model)
    {
        return haveQualifiedAuthorization(model, "alarm_notify");
    }

    /** @return Label that indicates missing server connection */
    public static Label createNoServerLabel()
    {
        final Label no_server = new Label("No Alarm Server Connection");
        no_server.setTextFill(Color.WHITE);
        no_server.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
        return no_server;
    }
}
