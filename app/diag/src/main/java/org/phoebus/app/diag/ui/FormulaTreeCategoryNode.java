package org.phoebus.app.diag.ui;

import javafx.scene.control.TreeItem;
import org.csstudio.apputil.formula.spi.FormulaFunction;

import java.util.ArrayList;
import java.util.Optional;

public class FormulaTreeCategoryNode extends TreeItem<FormulaTreeByCategoryNode> {
    public FormulaTreeCategoryNode(FormulaFunction firstChild) {
        super(new FormulaTreeByCategoryNode(firstChild.getCategory(), ""));
        addChild(firstChild);
    }

    public void addChild(FormulaFunction child) {
        getChildren().add(new FormulaTreeLeafNode(child));
    }
}
