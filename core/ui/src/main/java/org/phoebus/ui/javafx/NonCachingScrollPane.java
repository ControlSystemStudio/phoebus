/*
 * Copyright (C) 2023 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.ui.javafx;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.ScrollPaneSkin;
import javafx.scene.layout.StackPane;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link ScrollPane} subclass disabling caching on the (probably) viewport. Motivation is that
 * under certain circumstances the contents of a {@link ScrollPane} gets blurred when zooming.
 * See <a href="https://bugs.openjdk.org/browse/JDK-8089499">this OpenJDK bug</a> and this
 * <a href="https://github.com/ControlSystemStudio/phoebus/issues/2864">Github issue</a>.
 * The implementation - based on the OpenJDK bug ticket - depends on reflection to access a private field
 * in {@link ScrollPaneSkin}. As per November 2023 the issue is not fixed in JavaFX version 21.
 * <p>
 * In case the addressed field is not available or accessible (e.g. due JavaFX updates), exceptions are logged,
 * but not propagated, in which case this subclass inherits the behavior of {@link ScrollPane}.
 * </p>
 */
public class NonCachingScrollPane extends ScrollPane {

    public NonCachingScrollPane(Node content) {
        super(content);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        try {
            return new NonCachingScrollPaneSkin(this);
        } catch (Exception e) {
            // Java reflection magic failed, fall back to default skin.
            return super.createDefaultSkin();
        }
    }

    /**
     * {@link ScrollPaneSkin} subclass that attempts to retrieve the private field <code>viewRect</code>
     * of the {@link ScrollPaneSkin} class, and then set the caching of that skin to <code>false</code>.
     * If the field cannot be accessed, a {@link RuntimeException} is thrown in the constructor.
     */
    private static class NonCachingScrollPaneSkin extends ScrollPaneSkin {

        public NonCachingScrollPaneSkin(final ScrollPane scrollpane) {
            super(scrollpane);
            StackPane viewRect;
            try {
                Field viewRectField = ScrollPaneSkin.class.getDeclaredField("viewRect");
                viewRectField.setAccessible(true);
                viewRect = (StackPane) viewRectField.get(this);
                viewRect.setCache(false);
            }
            catch (Exception e) {
                Logger.getLogger(NonCachingScrollPane.class.getName()).log(Level.WARNING,
                        "Unable to find field viewRect via reflection",
                        e);
                throw new RuntimeException(e);
            }
        }
    }
}
