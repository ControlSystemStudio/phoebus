package org.phoebus.app.diag.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.csstudio.apputil.formula.spi.FormulaFunction;

import java.util.*;

/**
 * Controller for the Tree view of Formulas.
 */
public class FormulaTreeController {
    @FXML
    TreeTableColumn<FormulaTreeByCategoryNode, String> signature;
    @FXML
    TreeTableColumn<FormulaTreeByCategoryNode, String> description;

    @FXML
    TreeTableView<FormulaTreeByCategoryNode> treeTableView;

    @FXML
    public void initialize() {
        signature.setCellValueFactory(cellValue -> new ReadOnlyStringWrapper(cellValue.getValue().getValue().getSignature()));
        description.setCellValueFactory(cellValue -> new ReadOnlyStringWrapper(cellValue.getValue().getValue().getDescription()));

        FormulaTreeRootNode root = new FormulaTreeRootNode();
        ServiceLoader.load(FormulaFunction.class).forEach(func -> root.addChild(func));

        treeTableView.setRoot(root);
        treeTableView.setShowRoot(false);
    }
}
