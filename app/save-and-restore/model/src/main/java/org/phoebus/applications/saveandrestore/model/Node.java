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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModelProperty;
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
 * Class representing a node in a tree structure maintained by the jmasar service. Node types are
 * defined in enum {@link NodeType}.
 * 
 * The builder pattern supported by Lombok can be used to instantiate objects. The nodeType of a
 * Node object defaults to {@link NodeType#FOLDER}.
 * @author georgweiss
 *
 */
public class Node implements Comparable<Node>{
	
	@ApiModelProperty(required = false, value = "Database id of the node, defined by the server", allowEmptyValue = true)
	private int id;
	@ApiModelProperty(required = false, value = "Unique id of this node, defined in this class as a UUID. Client may override the default value through the setter. ", allowEmptyValue = true)
	@Builder.Default
	private final String uniqueId = UUID.randomUUID().toString();
	@ApiModelProperty(required = true, value = "Name of the folder or configuration")
	private String name;
	@ApiModelProperty(required = false, value = "Creation date, set by the server", allowEmptyValue = true)
	private Date created;
	@ApiModelProperty(required = false, value = "Last modified date, set by the server", allowEmptyValue = true)
	private Date lastModified;
	@ApiModelProperty(required = false, value = "Should be defined by the subclass.")
	@Builder.Default
	private NodeType nodeType = NodeType.FOLDER;
	@ApiModelProperty(required = true, value = "User name creating or modifying a node")
	private String userName;
	@ApiModelProperty(required = false, value = "Map of key/value pairs to be uses as collection of properties")
	private Map<String, String> properties;

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
