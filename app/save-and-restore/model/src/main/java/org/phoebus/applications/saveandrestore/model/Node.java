/** 
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Class representing a node in a tree structure maintained by the save-and-restore service. Node types are
 * defined in enum {@link NodeType}.
 * 
 * The builder pattern supported by Lombok can be used to instantiate objects. The nodeType of a
 * Node object defaults to {@link NodeType#FOLDER}.
 * @author georgweiss
 *
 */
public class Node implements Comparable<Node>{

	private int id;
	@Builder.Default
	private final String uniqueId = UUID.randomUUID().toString();
	private String name;
	private Date created;
	private Date lastModified;
	@Builder.Default
	private NodeType nodeType = NodeType.FOLDER;
	private String userName;
	private Map<String, String> properties;
	private List<Tag> tags;

	/**
	 * Do not change!!!
	 */
	public static final int ROOT_NODE_ID = 0;
	
	public void putProperty(String key, String value) {
		if(properties == null) {
			properties = new HashMap<>();
		}
		properties.put(key,  value);
	}
	
	public void removeProperty(String key) {
		if(properties != null) {
			properties.remove(key);
		}
	}
	
	public String getProperty(String key) {
		if(properties == null) {
			return null;
		}
		return properties.get(key);
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

	@Override
	public boolean equals(Object other) {
		if(other == null) {
			return false;
		}
		if(other instanceof Node) {
			Node otherNode = (Node)other;
			return nodeType.equals(otherNode.getNodeType()) &&
					uniqueId.equals(otherNode.getUniqueId());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeType, uniqueId);
	}
	
	/**
	 * Implements strategy where folders are sorted before configurations (save sets), and
	 * equal node types are sorted alphabetically.
	 * @param other The tree item to compare to
	 * @return -1 if this item is a folder and the other item is a save set,
	 * 1 if vice versa, and result of name comparison if node types are equal.
	 */
	@Override
	public int compareTo(Node other) {
		
		if(nodeType.equals(NodeType.FOLDER) && other.getNodeType().equals(NodeType.CONFIGURATION)){
			return -1;
		}
		else if(getNodeType().equals(NodeType.CONFIGURATION) && other.getNodeType().equals(NodeType.FOLDER)){
			return 1;
		}
		else{
			return getName().compareTo(other.getName());
		}
	}
}
