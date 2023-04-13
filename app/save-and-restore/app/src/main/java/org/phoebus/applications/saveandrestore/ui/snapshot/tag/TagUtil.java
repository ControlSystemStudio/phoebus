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
package org.phoebus.applications.saveandrestore.ui.snapshot.tag;

import javafx.scene.Node;
import org.phoebus.applications.saveandrestore.model.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link TagUtil} class provides rich information header of {@link Tag} as {@link Node}
 *
 * @author Genie Jhang <changj@frib.msu.edu>
 */

public class TagUtil {

    /**
     * Determines a list of {@link Tag}s that must occur in all specified {@link org.phoebus.applications.saveandrestore.model.Node}s
     *
     * @param nodes List of {@link org.phoebus.applications.saveandrestore.model.Node}s, each with a <code>null</code>, empty or
     *              non-empty list of {@link Tag}s.
     * @return A potentially empty list of {@link Tag}s.
     */
    public static List<Tag> getCommonTags(List<org.phoebus.applications.saveandrestore.model.Node> nodes) {
        // Construct a list containing all tags across all nodes. May contain duplicates.
        List<Tag> allTags = new ArrayList<>();
        nodes.stream().forEach(n -> {
            if (n.getTags() != null) {
                allTags.addAll(n.getTags());
            }
        });

        // List of common tags is constructed of tags having same number of occurrences as the size of the node list.
        List<Tag> commonTags = new ArrayList<>();
        allTags.forEach(t -> {
            if (Collections.frequency(allTags, t) == nodes.size() && !commonTags.contains(t)) {
                commonTags.add(t);
            }
        });

        return commonTags;
    }
}
