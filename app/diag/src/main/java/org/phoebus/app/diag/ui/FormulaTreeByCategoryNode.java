package org.phoebus.app.diag.ui;

import org.csstudio.apputil.formula.spi.FormulaFunction;

public class FormulaTreeByCategoryNode {

    // The model that contains the node, used to access all data
    // common to all nodes
    private FormulaFunction model;

    private final String signature;

    private final String description;

    public FormulaTreeByCategoryNode(String signature, String description) {
        this.signature = signature;
        this.description = description;
    }

    public FormulaTreeByCategoryNode(FormulaFunction model) {
        this.model = model;

        this.signature = model.getSignature();
        this.description = model.getDescription();

    }

    public String getSignature()
    {
        return signature;
    }

    public String getDescription()
    {
        return description;
    }
}
