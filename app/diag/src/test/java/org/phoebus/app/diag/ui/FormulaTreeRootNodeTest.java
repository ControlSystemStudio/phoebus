package org.phoebus.app.diag.ui;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import javafx.scene.control.TreeItem;
import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.VType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FormulaTreeRootNodeTest {

    public FormulaFunction createFormula(String name, String description, String category) {
        return new FormulaFunction() {
            @Override
            public String getCategory() {
                return category;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public List<String> getArguments() {
                return new ArrayList<>();
            }

            @Override
            public VType compute(VType... args) throws Exception {
                return null;
            }
        };
    }

    public List<String> getFormulaSignaturesFromCategory(TreeItem<FormulaTreeByCategoryNode> category) {
        return category.getChildren().stream()
                .map(child -> child.getValue())
                .map(form -> form.getSignature())
                .collect(Collectors.toList());
    }

    @Test
    public void testCreatingRootNodeGivesDummySignatureAndDescription()
    {
        FormulaTreeRootNode rootNode = new FormulaTreeRootNode();
        assertThat(rootNode.getValue().getSignature(), equalTo(""));
        assertThat(rootNode.getValue().getDescription(), equalTo(""));
    }

    @Test
    public void testCreatingRootNodeGivesEmptyRootNode()
    {
        FormulaTreeRootNode rootNode = new FormulaTreeRootNode();
        assertThat(rootNode.getChildren().size(), equalTo(0));
    }

    @Test
    public void testGivenNoExistingCategoryAddingChildFormulaToRootNodeCreatesCategory()
    {
        String categoryName = "CATEGORY";
        String formula1Name = "TEST_NAME";
        FormulaFunction func = createFormula(formula1Name, "TEST_DESC", categoryName);
        FormulaTreeRootNode rootNode = new FormulaTreeRootNode();
        rootNode.addChild(func);
        assertThat(rootNode.getChildren().size(), equalTo(1));

        TreeItem<FormulaTreeByCategoryNode> category = rootNode.getChildren().get(0);
        assertThat(category.getValue().getSignature(), equalTo(categoryName));
        assertThat(category.getValue().getDescription(), equalTo(""));

        assertThat(category.getChildren().size(), equalTo(1));

        List<String> formulaSignatures = getFormulaSignaturesFromCategory(category);
        assertTrue(formulaSignatures.contains(formula1Name + "()"));
    }

    @Test
    public void testGivenExistingCategoryAddingChildFormulaToRootNodeDoesNotCreateCategory()
    {
        String categoryName = "CATEGORY";
        String formula1Name = "TEST_NAME";
        String formula2Name = "TEST_NAME2";
        FormulaFunction firstFunc = createFormula(formula1Name, "TEST_DESC", categoryName);
        FormulaFunction secondFunc = createFormula(formula2Name, "TEST_DESC2", categoryName);

        FormulaTreeRootNode rootNode = new FormulaTreeRootNode();
        rootNode.addChild(firstFunc);
        assertThat(rootNode.getChildren().size(), equalTo(1));

        rootNode.addChild(secondFunc);
        assertThat(rootNode.getChildren().size(), equalTo(1));

        TreeItem<FormulaTreeByCategoryNode> category = rootNode.getChildren().get(0);
        assertThat(category.getChildren().size(), equalTo(2));

        List<String> formulaSignatures = getFormulaSignaturesFromCategory(category);
        assertTrue(formulaSignatures.contains(formula1Name + "()"));
        assertTrue(formulaSignatures.contains(formula2Name + "()"));
    }
}
