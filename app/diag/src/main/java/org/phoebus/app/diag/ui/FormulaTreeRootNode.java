package org.phoebus.app.diag.ui;

import javafx.scene.control.TreeItem;
import org.csstudio.apputil.formula.spi.FormulaFunction;

import java.util.ArrayList;

public class FormulaTreeRootNode extends TreeItem<FormulaTreeByCategoryNode> {
    ArrayList<FormulaTreeCategoryNode> categories = new ArrayList<>();

    public FormulaTreeRootNode() {
        super(new FormulaTreeByCategoryNode("", ""));
    }

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
