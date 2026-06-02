/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.csstudio.trends.databrowser3.persistence;

import org.csstudio.trends.databrowser3.model.FormulaInput;
import org.csstudio.trends.databrowser3.model.FormulaItem;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class XMLPersistenceTest {

    @Test
    public void testPvAndFormulaOrdering() throws Exception {

        Model model = new Model();

        PVItem pvItem1 = new PVItem("pvitem1", 1.0);
        FormulaInput formulaInput1 = new FormulaInput(pvItem1, "x1");
        FormulaItem formulaItem1 = new FormulaItem("formula1", "x1 * 2", new FormulaInput[]{formulaInput1});

        model.addItem(pvItem1);
        model.addItem(formulaItem1);

        PVItem pvItem2 = new PVItem("pvitem2", 1.0);
        FormulaInput formulaInput2 = new FormulaInput(pvItem2, "x2");

        formulaItem1 = (FormulaItem) model.getItem("formula1");

        formulaItem1.updateFormula("x1 + x2", new FormulaInput[]{formulaInput1, formulaInput2});

        // Add PV item referenced in formula after the update
        model.addItem(pvItem2);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLPersistence.write(model, baos);

        // Make sure the XML is readable
        XMLPersistence.load(new Model(), new ByteArrayInputStream(baos.toByteArray()));

    }
}
