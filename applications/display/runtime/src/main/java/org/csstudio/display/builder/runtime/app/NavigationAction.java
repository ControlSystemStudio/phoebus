/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import java.util.List;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Action to open 'previous' or 'next' display in back/forward navigation
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
// TODO Button with drop-down option
public abstract class NavigationAction extends Button
{
    /** Icons */
    private static final Image backward, forward;

    static
    {
        backward = new Image(NavigationAction.class.getResource("/icons/backward_nav.png").toExternalForm());
        forward = new Image(NavigationAction.class.getResource("/icons/forward_nav.png").toExternalForm());
    }

    /** @param instance {@link DisplayRuntimeInstance}
     *  @param navigation {@link DisplayNavigation} for that instance
     *  @return Button for navigating 'back'
     */
    public static Button createBackAction(final DisplayRuntimeInstance instance, final DisplayNavigation navigation)
    {
        return new NavigationAction(instance, navigation, backward)
        {
            @Override
            protected List<DisplayInfo> getDisplays()
            {
                return navigation.getBackwardDisplays();
            }

            @Override
            protected DisplayInfo getDisplayInfo(final int steps)
            {
                return navigation.goBackward(steps);
            }
        };
    }

    /** @param instance {@link DisplayRuntimeInstance}
     *  @param navigation {@link DisplayNavigation} for that instance
     *  @return Button for navigating 'forward'
     */
    public static Button createForewardAction(final DisplayRuntimeInstance instance, final DisplayNavigation navigation)
    {
        return new NavigationAction(instance, navigation, forward)
        {
            @Override
            protected List<DisplayInfo> getDisplays()
            {
                return navigation.getForwardDisplays();
            }

            @Override
            protected DisplayInfo getDisplayInfo(final int steps)
            {
                return navigation.goForward(steps);
            }
        };
    }

    private final DisplayRuntimeInstance instance;

    private NavigationAction(final DisplayRuntimeInstance instance, final DisplayNavigation navigation, final Image icon)
    {
        setGraphic(new ImageView(icon));
        this.instance = instance;

        // Then automatically enable/disable
        final DisplayNavigation.Listener listener = nav -> setDisable(getDisplays().isEmpty());
        navigation.addListener(listener);
        // Trigger initial enable/disable
        listener.displayHistoryChanged(navigation);

        setOnAction(event -> navigate(1));
    }

    /** @return List of back resp. forward displays */
    abstract protected List<DisplayInfo> getDisplays();

    /** @param steps Steps to navigate fore or back
     *  @return {@link DisplayInfo} at that location in navigation stack
     */
    abstract protected DisplayInfo getDisplayInfo(int steps);

    /** @param steps Steps to navigate */
    private void navigate(final int steps)
    {
        instance.loadDisplayFile(getDisplayInfo(steps));
    }
}
