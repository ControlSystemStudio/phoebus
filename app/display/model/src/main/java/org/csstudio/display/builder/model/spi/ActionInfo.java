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

package org.csstudio.display.builder.model.spi;

import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;

public interface ActionInfo extends Comparable<ActionInfo> {

    /**
     * If action type is not sufficient to determine match, implementations may add additional logic
     * to resolve match. For instance: legacy display formats may use a different string to define the action type.
     *
     * @param actionId Action id, e.g. open_display.
     * @return <code>true</code> if the input string is implemented by the {@link ActionInfo}.
     */
    default boolean matchesAction(String actionId) {
        return false;
    }

    /**
     * Non-null identifier, must be unique between all implementations.
     *
     * @return The action 'type'.
     */
    String getType();

    /**
     * Image shown in drop-down in editor and runtime.
     *
     * @return An {@link Image} representing the action in (for instance) editor and context menu.
     */
    Image getImage();

    /**
     * @return Default or user-defined description string. Implementations should define a non-empty string.
     */
    String getDescription();

    /**
     * @return Integer used to order actions in the display builder editor
     */
    default Integer getPriority() {
        return 100;
    }

    /**
     * @param description User-defined string, overriding the default.
     */
    void setDescription(String description);

    /**
     * Reads implementation specific XML.
     *
     * @param modelReader A {@link ModelReader}
     * @param actionXml   The {@link Element} holding the &lt;action&gt; tag data.
     * @throws Exception On failure
     */
    void readFromXML(final ModelReader modelReader, final Element actionXml) throws Exception;

    /**
     * Writes the body of the action tag, but not the action start/end tag.
     *
     * @param modelWriter A {@link ModelWriter}
     * @param writer      A {@link XMLStreamWriter}
     * @throws Exception On failure
     */
    void writeToXML(final ModelWriter modelWriter, final XMLStreamWriter writer) throws Exception;

    /**
     * Used to define action behavior if it depends on key modifiers, e.g. open display in specific target.
     *
     * @param event The {@link MouseEvent} holding information on potential modifier keys.
     */
    default void setModifiers(MouseEvent event) {
    }

    /**
     * @param executorService The {@link ExecutorService} responsible for running the action(s) defined
     *                        by the returned {@link MenuItem}s.
     * @param widget          The {@link Widget} defining the actions.
     * @return A {@link List} of {@link MenuItem}s for the action in a widget's context menu. All {@link MenuItem}s
     * must define the <code>onAction()</code>onAction() handler.
     * Defaults to <code>null</code>.
     */
    default List<MenuItem> getContextMenuItems(ExecutorService executorService, Widget widget) {
        return null;
    }

    /**
     * Comparator for the sake of sorting {@link ActionInfo}s. Uses {@link ActionInfo#getDescription()}.
     *
     * @param other the object to be compared.
     * @return Any occurrence of <code>null</code> in the {@link ActionInfo#getDescription()}
     * fields will return 0. Otherwise, comparison of {@link ActionInfo#getDescription()}.
     */
    @Override
    default int compareTo(ActionInfo other) {
        if (getDescription() == null || other.getDescription() == null) {
            return 0;
        }
        return getDescription().compareTo(other.getDescription());
    }

    /**
     * @return An {@link ActionHandler} matching the action. If none can be found, a {@link RuntimeException} is thrown.
     */
    default ActionHandler getActionHandler() {
        Optional<ServiceLoader.Provider<ActionHandler>> handler = ServiceLoader.load(ActionHandler.class).stream().filter(p -> p.get().matches(this)).findFirst();
        if (handler.isEmpty()) {
            throw new RuntimeException("No ActionHandler found for action " + getDescription());
        }
        return handler.get().get();
    }
}
