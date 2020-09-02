/**
 * Copyright (C) 2020 Facility for Rare Isotope Beams
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact Information: Facility for Rare Isotope Beam,
 *                      Michigan State University,
 *                      East Lansing, MI 48824-1321
 *                      http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore;

import javafx.scene.control.Alert;
import javafx.util.Pair;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;

import java.util.Stack;
import java.util.logging.Logger;

/**
 * {@link DirectoryUtilities} class provides APIs related to {@link Node} location
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class DirectoryUtilities {
    private static final SaveAndRestoreService saveAndRestoreService = (SaveAndRestoreService) ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().getBean("saveAndRestoreService");

    private static final Logger LOG = Logger.getLogger(SaveAndRestoreService.class.getName());

    public static final String HIERARCHY_SPLITTER = "▶";

    /**
     * Creates location string in the form of "A ▶ B ▶ ... ▶ C"
     *
     * @param endNode Endmost {@link Node} to start with
     * @param startFromParentNode if true, excludes {@param endNode} from the location {@link String}
     * @return a {@link String} containing location information
     */
    public static String CreateLocationString(Node endNode, boolean startFromParentNode) {
        String locationString = startFromParentNode ? "" : endNode.getName();

        Node node = endNode;
        while (true) {
            try {
                Node parentNode = saveAndRestoreService.getParentNode(node.getUniqueId());

                if (parentNode.getName().equals("Root folder")) {
                    break;
                }

                if (locationString.isEmpty()) {
                    locationString = parentNode.getName();
                } else {
                    locationString = parentNode.getName() + " " + HIERARCHY_SPLITTER + " " + locationString;
                }
                node = parentNode;
            } catch (Exception e) {
                String alertMessage = "Cannot retrieve the parent node of node: " + node.getName() + "(" + node.getUniqueId() + ")";

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText(alertMessage);
                alert.show();

                LOG.severe(alertMessage);

                e.printStackTrace();
                break;
            }
        }

        return locationString;
    }

    public static Pair<String, Stack<Node>> CreateLocationStringAndNodeStack(Node endNode, boolean startFromParentNode) {
        String locationString = "";
        Stack<Node> nodeStack = new Stack<>();
        if (!startFromParentNode) {
            locationString = endNode.getName();
            nodeStack.push(endNode);
        }

        Node node = endNode;
        while (true) {
            try {
                Node parentNode = saveAndRestoreService.getParentNode(node.getUniqueId());

                if (parentNode.getName().equals("Root folder")) {
                    break;
                }

                if (locationString.isEmpty()) {
                    locationString = parentNode.getName();
                } else {
                    locationString = parentNode.getName() + " " + HIERARCHY_SPLITTER + " " + locationString;
                }

                node = parentNode;
                nodeStack.push(node);
            } catch (Exception e) {
                String alertMessage = "Cannot retrieve the parent node of node: " + node.getName() + "(" + node.getUniqueId() + ")";

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText(alertMessage);
                alert.show();

                LOG.severe(alertMessage);

                e.printStackTrace();
                break;
            }
        }

        return new Pair<>(locationString, nodeStack);
    }
}