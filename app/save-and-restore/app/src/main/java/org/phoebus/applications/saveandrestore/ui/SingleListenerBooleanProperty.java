/*
 * This software is Copyright by the Board of Trustees of Michigan
 * State University (c) Copyright 2016.
 *
 * Contact Information:
 *   Facility for Rare Isotope Beam
 *   Michigan State University
 *   East Lansing, MI 48824-1321
 *   http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.ui;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

/**
 *
 * <code>SingleListenerBooleanProperty</code> is a boolean property which only allows a single listener to be added. All
 * subsequent listener will be ignored.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class SingleListenerBooleanProperty extends SimpleBooleanProperty {

    private boolean listenerSet;
    private ChangeListener<? super Boolean> listener;

    /**
     * Constructs a new property for the given bean with the given name and initial value.
     *
     * @param bean the bean to which the property belongs to
     * @param name the name of the property
     * @param initialValue the initial value of this property
     */
    public SingleListenerBooleanProperty(Object bean, String name, boolean initialValue) {
        super(bean, name, initialValue);
    }

    /**
     * Add a listener, which is always added to this property, even if one (or more) listener already exists.
     *
     * @param listener the listener to add
     */
    public void forceAddListener(ChangeListener<? super Boolean> listener) {
        super.addListener(listener);
    }

    /*
     * (non-Javadoc)
     * @see javafx.beans.property.BooleanPropertyBase#addListener(javafx.beans.value.ChangeListener)
     */
    @Override
    public void addListener(ChangeListener<? super Boolean> listener) {
        if (!listenerSet) {
            listenerSet = true;
            this.listener = listener;
            super.addListener(listener);
        }
    }

    /*
     * (non-Javadoc)
     * @see javafx.beans.property.BooleanPropertyBase#removeListener(javafx.beans.value.ChangeListener)
     */
    @Override
    public void removeListener(ChangeListener<? super Boolean> listener) {
        super.removeListener(listener);
        if (this.listener == listener) {
            listenerSet = false;
            this.listener = null;
        }
    }
}
