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
package org.phoebus.service.saveandrestore.persistence.dao.impl;

import org.phoebus.applications.saveandrestore.model.Tag;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class TagsRowMapper implements ResultSetExtractor<List<Tag>> {
    @Override
    public List<Tag> extractData(ResultSet resultSet) throws SQLException, DataAccessException {
        List<Tag> tags = new ArrayList<>();
        while (resultSet.next()) {
            Tag tag = Tag.builder()
                    .snapshotId(resultSet.getString("snapshot_id"))
                    .name(resultSet.getString("name"))
                    .comment(resultSet.getString("comment"))
                    .created(resultSet.getTimestamp("created"))
                    .userName(resultSet.getString("username"))
                    .build();

            tags.add(tag);
        }

        return tags;
    }
}
