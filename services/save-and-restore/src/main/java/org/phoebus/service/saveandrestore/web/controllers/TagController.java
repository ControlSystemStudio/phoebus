/**
 * Copyright (C) 2020 Facility for Rare Isotope Beams
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * <p>
 * Contact Information: Facility for Rare Isotope Beam,
 * Michigan State University,
 * East Lansing, MI 48824-1321
 * http://frib.msu.edu
 */
package org.phoebus.service.saveandrestore.web.controllers;

import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.TagData;
import org.phoebus.applications.saveandrestore.model.websocket.MessageType;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.WebSocketMessage;

import java.security.Principal;
import java.util.List;

/**
 * {@link TagController} class for supporting REST-ful APIs for tag
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

@RestController
@SuppressWarnings("unused")
public class TagController extends BaseController {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private WebSocketHandler webSocketHandler;

    /**
     * @return A {@link List} of all {@link Tag}s.
     */
    @GetMapping("/tags")
    public List<Tag> getTags() {
        return nodeDAO.getAllTags();
    }

    /**
     * Adds a {@link Tag} to specified list of target {@link Node}s. The {@link Tag} contained
     * in tagData must be non-null, and its name must be non-null and non-empty.
     *
     * @param tagData   See {@link TagData}
     * @param principal {@link Principal} of authenticated user.
     * @return The list of updated {@link Node}s
     */
    @PostMapping("/tags")
    @PreAuthorize("@authorizationHelper.mayAddOrDeleteTag(#tagData, #root)")
    public List<Node> addTag(@RequestBody TagData tagData,
                             Principal principal) {
        tagData.getTag().setUserName(principal.getName());
        List<Node> taggedNodes = nodeDAO.addTag(tagData);
        taggedNodes.forEach(n -> webSocketHandler.sendMessage(new SaveAndRestoreWebSocketMessage(MessageType.NODE_UPDATED, n)));
        return taggedNodes;
    }

    /**
     * Removes a {@link Tag} from specified list of target {@link Node}s. The {@link Tag} contained
     * in tagData must be non-null, and its name must be non-null and non-empty.
     *
     * @param tagData See {@link TagData}
     * @return The list of updated {@link Node}s
     */
    @DeleteMapping("/tags")
    @PreAuthorize("@authorizationHelper.mayAddOrDeleteTag(#tagData, #root)")
    public List<Node> deleteTag(@RequestBody TagData tagData) {
        List<Node> untaggedNodes = nodeDAO.deleteTag(tagData);
        untaggedNodes.forEach(n -> webSocketHandler.sendMessage(new SaveAndRestoreWebSocketMessage(MessageType.NODE_UPDATED, n)));
        return untaggedNodes;
    }
}
