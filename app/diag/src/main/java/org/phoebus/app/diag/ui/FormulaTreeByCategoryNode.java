package org.phoebus.app.diag.ui;

/**
 * A class for holding information about a formula to be displayed in the Formula Tree.
 */
public class FormulaTreeByCategoryNode {
    private String signature;
    private String description;

    public FormulaTreeByCategoryNode(String signature, String description) {
        this.signature = signature;
        this.description = description;
    }

    /**
     * Get the signature of the formula or the name of a category.
     * @return The signature of the formula or category name e.g. sin(x) or math
     */
    public String getSignature() { return signature; };

    /**
     * Get the description of the formula or category.
     * @return The description of the formula or category.
     */
    public String getDescription() { return description; };
}
