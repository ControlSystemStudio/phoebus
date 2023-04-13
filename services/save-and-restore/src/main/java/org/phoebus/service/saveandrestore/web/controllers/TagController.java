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
package org.phoebus.service.saveandrestore.web.controllers;

import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.TagData;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * {@link TagController} class for supporting RESTful APIs for tag
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
     * @param tagData See {@link TagData}
     * @return The list of updated {@link Node}s
     */
    @PostMapping("/tags")
    public List<Node> addTag(@RequestBody TagData tagData){
        if(tagData.getTag() == null ||
                tagData.getTag().getName() == null ||
                tagData.getTag().getName().isEmpty() ||
                tagData.getUniqueNodeIds() == null){
            throw new IllegalArgumentException("Cannot add tag, data invalid");
        }
        return nodeDAO.addTag(tagData);
    }

    /**
     * Removes a {@link Tag} from specified list of target {@link Node}s. The {@link Tag} contained
     *      * in tagData must be non-null, and its name must be non-null and non-empty.
     * @param tagData See {@link TagData}
     * @return The list of updated {@link Node}s
     */
    @DeleteMapping("/tags")
    public List<Node> deleteTag(@RequestBody TagData tagData){
        if(tagData.getTag() == null ||
                tagData.getTag().getName() == null ||
                tagData.getTag().getName().isEmpty() ||
                tagData.getUniqueNodeIds() == null){
            throw new IllegalArgumentException("Cannot add tag, data invalid");
        }
        return nodeDAO.deleteTag(tagData);
    }
}
