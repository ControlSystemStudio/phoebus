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
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/tags")
    public List<Tag> getTags() {
        return nodeDAO.getAllTags();
    }

    /**
     * Adds a {@link Tag} to specified list of target {@link Node}s. The {@link Tag} contained
     * in tagData must be non-null, and its name must be non-null and non-empty.
     *
     * @param tagData        See {@link TagData}
     * @param userName Must be non-null and non-empty if authentication/authorization is disabled.
     * @param authentication {@link Authentication} of authenticated user.
     * @return The list of updated {@link Node}s
     */
    @PostMapping("/tags")
    @PreAuthorize("hasRole(this.roleAdmin) or (hasRole(this.roleUser) and @authorizationHelper.mayAddOrDeleteTag(#tagData, #authentication))")
    public List<Node> addTag(@RequestBody TagData tagData,
                             @RequestParam(name = "username", required = false) String userName,
                             Authentication authentication) {
        if(authentication == null && (userName == null || userName.isEmpty())){
            throw new IllegalArgumentException("Cannot determine username for tag");
        }
        tagData.getTag().setUserName(authentication == null ? userName : authentication.getName());
        return nodeDAO.addTag(tagData);
    }

    /**
     * Removes a {@link Tag} from specified list of target {@link Node}s. The {@link Tag} contained
     * * in tagData must be non-null, and its name must be non-null and non-empty.
     *
     * @param tagData See {@link TagData}
     * @return The list of updated {@link Node}s
     */
    @DeleteMapping("/tags")
    @PreAuthorize("hasRole(this.roleAdmin) or (hasRole(this.roleUser) and @authorizationHelper.mayAddOrDeleteTag(#tagData, #authentication))")
    public List<Node> deleteTag(@RequestBody TagData tagData, Authentication authentication) {
        return nodeDAO.deleteTag(tagData);
    }
}
