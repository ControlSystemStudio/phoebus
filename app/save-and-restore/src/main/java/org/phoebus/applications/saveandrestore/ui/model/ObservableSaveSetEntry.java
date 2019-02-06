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
package org.phoebus.applications.saveandrestore.ui.model;

import javafx.beans.property.SimpleObjectProperty;
import se.esss.ics.masar.model.ConfigPv;
import se.esss.ics.masar.model.Provider;

/**
 * An observable built around a {@link ConfigPv} to simplify the use of
 * javafx controls
 *
 * @author Kunal Shroff
 * @author Georg Weiss, ESS
 *
 */
public class ObservableSaveSetEntry {

	private final SimpleObjectProperty<ConfigPv> configPv;

	public ObservableSaveSetEntry(ConfigPv configPv) {
		this.configPv = new SimpleObjectProperty<>(configPv);
	}

	public String getPvname() {
		return configPv.get().getPvName();
	}

	public void setPvname(String pvName) {
		configPv.get().setPvName(pvName);
	}

	public Provider getProvider() {
		return configPv.get().getProvider();
	}

	public void setProvider(Provider provider) {
		configPv.get().setProvider(provider);
	}

	public ConfigPv getConfigPv() {
		return this.configPv.get();
	}
	

	@Override
	public String toString() {
		return getPvname() + "(" + getProvider().toString() + ")";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof ObservableSaveSetEntry))
			return false;
		
		ObservableSaveSetEntry entry = (ObservableSaveSetEntry)obj;
		
		return entry.getPvname().equals(getPvname());
	}
	
	@Override
	public int hashCode() {
		return getPvname().hashCode();
	}
}