package org.phoebus.app.diag.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.csstudio.apputil.formula.spi.FormulaFunction;

import java.util.*;

/**
 * Controller for the Tree view of Channels based on a set of selected properties
 * 
 * @author Dominic Oram
 *
 */
public class FormulaTreeController {
    @FXML
    TreeTableColumn<FormulaTreeByCategoryNode, String> signature;
    @FXML
    TreeTableColumn<FormulaTreeByCategoryNode, String> description;

    @FXML
    TreeTableView<FormulaTreeByCategoryNode> treeTableView;

    private Collection<FormulaFunction> formulas = Collections.emptyList();

    @FXML
    public void initialize() {
        signature.setCellValueFactory(cellValue -> new ReadOnlyStringWrapper(cellValue.getValue().getValue().getSignature()));

        description.setCellValueFactory(cellValue -> new ReadOnlyStringWrapper(cellValue.getValue().getValue().getDescription()));

        TreeItem root = new TreeItem(new FormulaTreeByCategoryNode("TEST", "TEST_DESC"));

        ArrayList<TreeItem<FormulaTreeByCategoryNode>> children = new ArrayList<>();
        for (FormulaFunction func : ServiceLoader.load(FormulaFunction.class))
        {
            children.add(new TreeItem(new FormulaTreeByCategoryNode(func)));
        }
        root.getChildren().setAll(children);
        treeTableView.setRoot(root);
        treeTableView.setShowRoot(false);
        dispose();
    }

    @FXML
    public void dispose() {

    }

    @FXML
    public void createContextMenu() {

    }

}
