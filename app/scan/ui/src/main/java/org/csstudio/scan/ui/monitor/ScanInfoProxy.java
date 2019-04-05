/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.monitor;

import java.time.Instant;

import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanState;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

/** Property-based proxy for a {@link ScanInfo}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ScanInfoProxy
{
    final SimpleLongProperty id;
    final SimpleStringProperty name;
    final SimpleObjectProperty<Instant> created;
    final SimpleObjectProperty<ScanState> state;
    final SimpleIntegerProperty percent;
    final SimpleStringProperty runtime;
    final SimpleObjectProperty<Instant> finish;
    final SimpleStringProperty command;
    final SimpleStringProperty error;
    volatile ScanInfo info;

    public ScanInfoProxy(final ScanInfo info)
    {
        id = new SimpleLongProperty(info.getId());
        name = new SimpleStringProperty(info.getName());
        created = new SimpleObjectProperty<>(info.getCreated());
        state = new SimpleObjectProperty<>(info.getState());
        percent = new SimpleIntegerProperty(info.getPercentage());
        runtime = new SimpleStringProperty(info.getRuntimeText());
        finish = new SimpleObjectProperty<>(info.getFinishTime());
        command = new SimpleStringProperty(info.getCurrentCommand());
        error = new SimpleStringProperty(info.getError().orElse(""));
        this.info = info;
    }

    /** @return true if state changed */
    boolean updateFrom(final ScanInfo info)
    {
        final boolean changed = state.get() != info.getState();
        id.set(info.getId());
        name.set(info.getName());
        created.set(info.getCreated());
        state.set(info.getState());
        percent.set(info.getPercentage());
        runtime.set(info.getRuntimeText());
        finish.set(info.getFinishTime());
        command.set(info.getCurrentCommand());
        error.set(info.getError().orElse(""));
        this.info = info;

        return changed;
    }
}