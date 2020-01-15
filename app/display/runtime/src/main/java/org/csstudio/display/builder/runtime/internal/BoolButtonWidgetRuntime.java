/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.runtime.internal;


import org.csstudio.display.builder.model.widgets.BoolButtonWidget;
import org.csstudio.display.builder.model.widgets.KnobWidget;
import org.csstudio.display.builder.runtime.PVNameToValueBinding;
import org.csstudio.display.builder.runtime.WidgetRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime for a {@link org.csstudio.display.builder.model.widgets.BoolButtonWidget} which supports
 * an optional read-back PV used to set the visual status of the button.
 */
public class BoolButtonWidgetRuntime extends WidgetRuntime<BoolButtonWidget> {

    private final List<PVNameToValueBinding> bindings = new ArrayList<>();

    @Override
    public void start()
    {
        super.start();
        if(!widget.propReadbackPVName().getValue().isEmpty()){
            bindings.add(new PVNameToValueBinding(this, widget.propReadbackPVName(), widget.propReadbackPVValue()));
        }
    }

    @Override
    public void stop ( ) {
        bindings.stream().forEach(PVNameToValueBinding::dispose);
        super.stop();
    }
}
