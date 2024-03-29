/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.applications.saveandrestore.model;


public class Configuration {

    private Node configurationNode;
    private ConfigurationData configurationData;

    public Node getConfigurationNode() {
        return configurationNode;
    }

    public void setConfigurationNode(Node configurationNode) {
        this.configurationNode = configurationNode;
    }

    public ConfigurationData getConfigurationData() {
        return configurationData;
    }

    public void setConfigurationData(ConfigurationData configurationData) {
        this.configurationData = configurationData;
    }

    @Override
    public boolean equals(Object other){
        if(!(other instanceof Configuration)){
            return false;
        }
        return configurationNode.equals(((Configuration) other).getConfigurationNode());
    }

    @Override
    public int hashCode(){
        return configurationNode.hashCode();
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder{

        private Configuration configuration;

        private Builder(){
            configuration = new Configuration();
        }

        public Configuration.Builder configurationData(ConfigurationData configurationData){
            configuration.setConfigurationData(configurationData);
            return this;
        }

        public Configuration.Builder configurationNode(Node configurationNode){
            configuration.setConfigurationNode(configurationNode);
            return this;
        }

        public Configuration build(){
            return configuration;
        }
    }
}
