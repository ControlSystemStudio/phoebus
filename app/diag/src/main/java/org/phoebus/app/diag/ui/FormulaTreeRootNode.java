package org.phoebus.app.diag.ui;

import javafx.scene.control.TreeItem;
import org.csstudio.apputil.formula.spi.FormulaFunction;

import java.util.ArrayList;

/**
 * The root node for the formula tree. The tree view requires a single root note.
 * In this case the node contains a child list of {@link FormulaTreeCategoryNode} children.
 */
public class FormulaTreeRootNode extends TreeItem<FormulaTreeByCategoryNode> {
    private ArrayList<FormulaTreeCategoryNode> categories = new ArrayList<>();

    /**
     * Creates the root node with a dummy signature and description as it will not be visible to the user.
     */
    public FormulaTreeRootNode() {
        super(new FormulaTreeByCategoryNode("", ""));
    }

    /**
     * Adds a formula into the tree.
     * The category on the formula will be inspected and the formula will be added to the correct {@link FormulaTreeCategoryNode}.
     * If a corresponding {@link FormulaTreeCategoryNode} does not exist one will be created.
     * @param child The formula to add into the tree.
     */
    public void addChild(FormulaFunction child) {
        for (FormulaTreeCategoryNode category : categories) {
            if (category.getValue().getSignature().equals(child.getCategory())) {
                category.addChild(child);
                return;
            }
        }
        FormulaTreeCategoryNode newCategory = new FormulaTreeCategoryNode(child);
        categories.add(newCategory);
        this.getChildren().add(newCategory);
    }
}
