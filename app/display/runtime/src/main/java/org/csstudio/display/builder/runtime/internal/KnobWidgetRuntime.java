/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.runtime.internal;


import java.util.ArrayList;
import java.util.List;

import org.csstudio.display.builder.model.widgets.KnobWidget;
import org.csstudio.display.builder.runtime.PVNameToValueBinding;
import org.csstudio.display.builder.runtime.WidgetRuntime;


/**
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 6 Oct 2017
 */
public class KnobWidgetRuntime extends WidgetRuntime<KnobWidget> {

    private final List<PVNameToValueBinding> bindings = new ArrayList<>();

    @Override
    public void start()
    {
        super.start();
        bindings.add(new PVNameToValueBinding(this, widget.propReadbackPVName(), widget.propReadbackPVValue()));
    }

    @Override
    public void stop ( ) {
        bindings.stream().forEach(PVNameToValueBinding::dispose);
        super.stop();
    }
}
