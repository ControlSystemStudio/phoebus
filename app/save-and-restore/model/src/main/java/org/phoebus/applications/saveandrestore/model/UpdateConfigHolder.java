/*
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

import java.util.List;

/**
 * @author georgweiss
 * Created 20 Mar 2019
 */

/**
 * A wrapper class used in some calls to the JMasar REST end points.
 * @author georgweiss
 * Created 17 May 2019
 */
public class UpdateConfigHolder {

	private Node config;
	private List<ConfigPv> configPvList;

	public Node getConfig() {
		return config;
	}

	public void setConfig(Node config) {
		this.config = config;
	}

	public List<ConfigPv> getConfigPvList() {
		return configPvList;
	}

	public void setConfigPvList(List<ConfigPv> configPvList) {
		this.configPvList = configPvList;
	}

	public static Builder builder(){
		return new Builder();
	}

	public static class Builder{

		private UpdateConfigHolder updateConfigHolder;

		private Builder(){
			updateConfigHolder = new UpdateConfigHolder();
		}

		public Builder config(Node config){
			updateConfigHolder.setConfig(config);
			return this;
		}

		public Builder configPvList(List<ConfigPv> configPvList){
			updateConfigHolder.setConfigPvList(configPvList);
			return this;
		}

		public UpdateConfigHolder build(){
			return updateConfigHolder;
		}
	}
}
