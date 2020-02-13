package org.phoebus.app.diag.ui;

public class FormulaTreeByCategoryNode {
    private String signature;
    private String description;
    public FormulaTreeByCategoryNode(String signature, String description) {
        this.signature = signature;
        this.description = description;
    }
    public String getSignature() { return signature; };
    public String getDescription() { return description; };
}
