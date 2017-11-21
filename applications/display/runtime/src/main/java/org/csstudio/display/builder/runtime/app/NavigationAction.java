/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.display.builder.runtime.Messages;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Action to open 'previous' or 'next' display in back/forward navigation
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public abstract class NavigationAction extends SplitMenuButton
{
    /** Icons */
    private static final Image backward, forward;

    static
    {
        backward = ImageCache.getImage(NavigationAction.class, "/icons/backward_nav.png");
        forward = ImageCache.getImage(NavigationAction.class, "/icons/forward_nav.png");
    }

    /** @param instance {@link DisplayRuntimeInstance}
     *  @param navigation {@link DisplayNavigation} for that instance
     *  @return Button for navigating 'back'
     */
    public static ButtonBase createBackAction(final DisplayRuntimeInstance instance, final DisplayNavigation navigation)
    {
        return new NavigationAction(instance, navigation, Messages.NavigateBack_TT, backward)
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
    public static ButtonBase createForewardAction(final DisplayRuntimeInstance instance, final DisplayNavigation navigation)
    {
        return new NavigationAction(instance, navigation, Messages.NavigateForward_TT, forward)
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

    private NavigationAction(final DisplayRuntimeInstance instance, final DisplayNavigation navigation,
                             final String tooltip, final Image icon)
    {
        this.instance = instance;
        setGraphic(new ImageView(icon));
        // Don't react to '&' etc. in display names
        setMnemonicParsing(false);
        setTooltip(new Tooltip(tooltip));

        // Automatically enable/disable
        final DisplayNavigation.Listener listener = nav -> updateUI(nav);
        navigation.addListener(listener);
        // Trigger initial enable/disable
        listener.displayHistoryChanged(navigation);

        // Plain button click navigates one step
        setOnAction(event -> navigate(1));
    }

    private void updateUI(final DisplayNavigation navigation)
    {
        final List<DisplayInfo> displays = getDisplays();
        final int N = displays.size();
        if (N<=0)
        {
            setDisable(true);
            getItems().clear();
        }
        else
        {
            setDisable(false);
            final List<MenuItem> items = new ArrayList<>(N);
            for (int i=0; i<N; ++i)
                items.add(createNavigationItem(displays.get(N-i-1), i+1));
            getItems().setAll(items);
        }
    }

    private MenuItem createNavigationItem(final DisplayInfo info, final int steps)
    {
        final MenuItem item = new MenuItem(info.getName());
        item.setOnAction(event -> navigate(steps));
        return item;
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
