package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.ui.undo.UndoableAction;

public class ExchangeAxesUndoableAction extends UndoableAction {
    Model model;
    int axisIndex1;
    int axisIndex2;

    /**
     * @param name Name used to show action in undo/redo UI
     * @param model Data Browser model containing the axes whose positions to exchange
     * @param axisIndex1 Index of the first of the axes whose positions to exchange
     * @param axisIndex2 Index of the second of the axes whose positions to exchange
     */
    public ExchangeAxesUndoableAction(String name,
                                      Model model,
                                      int axisIndex1,
                                      int axisIndex2) {
        super(name);
        this.model = model;
        this.axisIndex1 = axisIndex1;
        this.axisIndex2 = axisIndex2;
    }

    @Override
    public void run() {
        model.exchangeAxes(axisIndex1, axisIndex2);

    }

    @Override
    public void undo() {
        model.exchangeAxes(axisIndex2, axisIndex1);
    }
}