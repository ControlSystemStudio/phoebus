/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.configuration;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.PvCompareMode;

import java.util.Objects;

/**
 * Wrapper around a {@link ConfigPv} instance for the purpose of facilitating
 * configuration and data binding in (for instance) a {@link javafx.scene.control.TableView}.
 */
public class ConfigPvEntry implements Comparable<ConfigPvEntry> {

    private final StringProperty pvNameProperty;
    private final StringProperty readBackPvNameProperty;
    private final BooleanProperty readOnlyProperty;
    private final ObjectProperty<PvCompareMode> pvCompareModeProperty;
    private ObjectProperty<Double> toleranceProperty;

    public ConfigPvEntry(ConfigPv configPv) {
        this.pvNameProperty = new SimpleStringProperty(this, "pvNameProperty", configPv.getPvName());
        this.readBackPvNameProperty = new SimpleStringProperty(configPv.getReadbackPvName());
        this.readOnlyProperty = new SimpleBooleanProperty(configPv.isReadOnly());
        this.pvCompareModeProperty = new SimpleObjectProperty(configPv.getPvCompareMode());
        this.toleranceProperty = configPv.getTolerance() != null ?
                new SimpleObjectProperty<>(configPv.getTolerance()) : new SimpleObjectProperty<>(null);
    }

    public StringProperty getPvNameProperty() {
        return pvNameProperty;
    }

    public StringProperty getReadBackPvNameProperty() {
        return readBackPvNameProperty;
    }

    public BooleanProperty getReadOnlyProperty() {
        return readOnlyProperty;
    }

    public ObjectProperty<PvCompareMode> getPvCompareModeProperty() {
        return pvCompareModeProperty;
    }

    public ObjectProperty<Double> getToleranceProperty() {
        return toleranceProperty;
    }

    public void setPvNameProperty(String pvNameProperty) {
        this.pvNameProperty.set(pvNameProperty);
    }

    public void setReadBackPvNameProperty(String readBackPvNameProperty) {
        this.readBackPvNameProperty.set(readBackPvNameProperty);
    }

    public void setReadOnlyProperty(boolean readOnlyProperty) {
        this.readOnlyProperty.set(readOnlyProperty);
    }

    public void setPvCompareModeProperty(PvCompareMode pvCompareModeProperty) {
        this.pvCompareModeProperty.set(pvCompareModeProperty);
    }

    public void setToleranceProperty(Double toleranceProperty) {
        this.toleranceProperty.set(toleranceProperty );
    }

    public ConfigPv toConfigPv() {
        return ConfigPv.builder()
                .pvName(pvNameProperty.get())
                .readbackPvName(readBackPvNameProperty.get())
                .readOnly(readOnlyProperty.get())
                .pvCompareMode(pvCompareModeProperty.get())
                .tolerance(toleranceProperty.get())
                .build();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ConfigPvEntry otherConfigPv) {
            return Objects.equals(pvNameProperty, otherConfigPv.getPvNameProperty()) &&
                    Objects.equals(readBackPvNameProperty, otherConfigPv.getReadBackPvNameProperty()) &&
                    Objects.equals(readOnlyProperty.get(), otherConfigPv.getReadOnlyProperty().get());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pvNameProperty, readBackPvNameProperty, readOnlyProperty);
    }

    @Override
    public int compareTo(ConfigPvEntry other) {
        return pvNameProperty.get().compareTo(other.getPvNameProperty().get());
    }
}
