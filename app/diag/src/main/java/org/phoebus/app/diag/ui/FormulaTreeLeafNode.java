package org.phoebus.app.diag.ui;

import javafx.scene.control.TreeItem;
import org.csstudio.apputil.formula.spi.FormulaFunction;

public class FormulaTreeLeafNode extends TreeItem<FormulaTreeByCategoryNode> {
    public FormulaTreeLeafNode(FormulaFunction model) {
        super(new FormulaTreeByCategoryNode(model.getSignature(), model.getDescription()));
    }
}
