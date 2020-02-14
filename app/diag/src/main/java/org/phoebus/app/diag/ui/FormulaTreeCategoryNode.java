package org.phoebus.app.diag.ui;

import javafx.scene.control.TreeItem;
import org.csstudio.apputil.formula.spi.FormulaFunction;

/**
 * A category node in the Formula Tree.
 * A category is a group that can contain a number of children of type {@link FormulaFunction}.
 */
public class FormulaTreeCategoryNode extends TreeItem<FormulaTreeByCategoryNode> {
    /**
     * Create a category node.
     * The name of the node will be the category of the provided function, this function will also be added as a child
     * of this category.
     * @param firstChild The first child of this category.
     */
    public FormulaTreeCategoryNode(FormulaFunction firstChild) {
        super(new FormulaTreeByCategoryNode(firstChild.getCategory(), ""));
        addChild(firstChild);
    }

    /**
     * Add a new child function to the category.
     * This function assumes that the check has already been done as to whether the the function belongs in this category.
     * @param child The function to add to this category.
     */
    public void addChild(FormulaFunction child) {
        FormulaTreeByCategoryNode leafNode = new FormulaTreeByCategoryNode(child.getSignature(), child.getDescription());
        getChildren().add(new TreeItem(leafNode));
    }
}
