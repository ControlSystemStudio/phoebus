/**
 * Copyright (C) 2018 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Class representing a node in a tree structure maintained by the save-and-restore service. Node types are
 * defined in enum {@link NodeType}.
 *
 * @author georgweiss
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Node implements Comparable<Node>, Serializable {

    private String uniqueId;
    private String name;
    private String description;
    private Date created;
    private Date lastModified;
    private NodeType nodeType = NodeType.FOLDER;
    private String userName;
    private List<Tag> tags;

    @Deprecated
    private Map<String, String> properties;

    /**
     * Do not change!!!
     */
    public static final String ROOT_FOLDER_UNIQUE_ID = "44bef5de-e8e6-4014-af37-b8f6c8a939a2";

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Deprecated
    public Map<String, String> getProperties() {
        return properties;
    }

    @Deprecated
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public boolean hasTag(String tagName) {
        if (tags == null || tags.isEmpty()) {
            return false;
        }
        return tags.stream().anyMatch(t -> t.getName().equals(tagName));
    }

    public void addTag(Tag tag) {
        if (tags == null) {
            tags = new ArrayList<>();
        }

        if (tags.stream().noneMatch(item -> item.getName().equals(tag.getName()))) {
            tags.add(tag);
        }
    }

    public void removeTag(Tag tag) {
        if (tags != null) {
            tags.stream()
                    .filter(item -> item.getName().equals(tag.getName()))
                    .findFirst()
                    .ifPresent(item -> tags.remove(item));
        }
    }

    /**
     * Implements strategy where the node type ordinal is considered first, then
     * name in lower case.
     *
     * @param other The tree item to compare to
     * @return -1 if this item is a folder and the other item is a configuration,
     * 1 if vice versa, and result of name comparison if node types are equal.
     */
    @Override
    public int compareTo(Node other) {
        return Comparator.comparing(Node::getNodeType)
                .thenComparing((Node n) -> n.getName().toLowerCase()).compare(this, other);
    }

    @Override
    public boolean equals(Object other) {
        if(other == null) {
            return false;
        }
        if(other instanceof Node) {
            Node otherNode = (Node)other;
            return uniqueId.equals(otherNode.getUniqueId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Node node;

        private Builder() {
            node = new Node();
        }

        public Builder uniqueId(String uniqueId) {
            node.setUniqueId(uniqueId);
            return this;
        }

        public Builder name(String name) {
            node.setName(name);
            return this;
        }

        public Builder userName(String userName) {
            node.setUserName(userName);
            return this;
        }

        public Builder created(Date created) {
            node.setCreated(created);
            return this;
        }

        public Builder lastModified(Date lastModified) {
            node.setLastModified(lastModified);
            return this;
        }

        public Builder nodeType(NodeType nodeType) {
            node.setNodeType(nodeType);
            return this;
        }

        public Builder tags(List<Tag> tags) {
            node.setTags(tags);
            return this;
        }

        public Builder description(String description) {
            node.setDescription(description);
            return this;
        }

        public Node build() {
            return node;
        }
    }
}
