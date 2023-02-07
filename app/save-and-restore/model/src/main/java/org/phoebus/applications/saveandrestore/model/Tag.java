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
package org.phoebus.applications.saveandrestore.model;

/**
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Tag implements Comparable<Tag>, Serializable {

    public static final String GOLDEN = "golden";

    private String name;
    private String comment;
    private Date created;
    private String userName;
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public int compareTo(Tag otherTag) {
        return name.equals(otherTag.getName()) ? 1 : 0;
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder{
        private Tag tag;

        private Builder(){
            tag = new Tag();
        }

        public Builder name(String name){
            tag.setName(name);
            return this;
        }

        public Builder comment(String comment){
            tag.setComment(comment);
            return this;
        }

        public Builder created(Date created){
            tag.setCreated(created);
            return this;
        }

        public Builder userName(String userName){
            tag.setUserName(userName);
            return this;
        }

        public Tag build(){
            return tag;
        }

    }

    public static Tag goldenTag(String userName){
        return Tag.builder().name(GOLDEN).userName(userName).created(new Date()).build();
    }
}
