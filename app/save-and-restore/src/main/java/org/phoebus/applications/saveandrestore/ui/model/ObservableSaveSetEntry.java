package org.phoebus.applications.saveandrestore.ui.model;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * An observable built around a {@link SaveSetEntry} to simplify the use of
 * javafx controls
 *
 * @author Kunal Shroff
 *
 */
public class ObservableSaveSetEntry {

	private final SimpleStringProperty pvName;
	private final SimpleObjectProperty<EpicsProvider> epicsProvider;

	ObservableSaveSetEntry(SaveSetEntry saveSetEntry) {
		this.pvName = new SimpleStringProperty(saveSetEntry.getPvName());
		this.epicsProvider = new SimpleObjectProperty<>(saveSetEntry.getEpicsProvider());
	}

	public String getPVName() {
		return pvName.get();
	}

	public void setPvname(String pvName) {
		this.pvName.set(pvName);
	}

	public EpicsProvider getEpicsProvider() {
		return epicsProvider.getValue();
	}

	public void setEpicsProvider(EpicsProvider provider) {
		epicsProvider.set(provider);
	}

	public SaveSetEntry getEntry() {
		return new SaveSetEntry(getPVName(), getEpicsProvider());
	}

	@Override
	public String toString() {
		return pvName.get() + "(" + epicsProvider.toString() + ")";
	}
}