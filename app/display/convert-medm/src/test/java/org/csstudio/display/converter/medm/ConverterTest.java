package org.csstudio.display.converter.medm;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.properties.ScriptPV;
import org.csstudio.display.builder.model.rules.RuleInfo;
import org.csstudio.display.builder.model.rules.RuleInfo.ExpressionInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SuppressWarnings("nls")
public class ConverterTest
{
    @BeforeAll
    public static void init()
    {
        final Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        for (Handler handler : root.getHandlers())
            handler.setLevel(root.getLevel());
    }

    @Test
    public void testConverter() throws Exception
    {
        // Convert into tmp file
        final String filename = ConverterTest.class.getResource("/Main_XXXX.adl").getFile();
        if (filename.isEmpty())
            throw new Exception("Cannot obtain test file");

        final File output = File.createTempFile("Main_XXX", ".bob");
        output.deleteOnExit();
        new Converter(new File(filename), output);

        // Dump the resulting *.bob file
        final BufferedReader dump = new BufferedReader(new FileReader(output));
        dump.lines().forEach(System.out::println);
        dump.close();

        // Read into model
        final ModelReader reader = new ModelReader(new FileInputStream(output));
        final DisplayModel model = reader.readModel();

        // Perform some tests
        testCalcRule(model);
    }

    private void testCalcRule(final DisplayModel model)
    {
        for (Widget widget : model.runtimeChildren().getValue())
        {
            if (! widget.getName().equals("composite #351"))
                continue;

            final List<RuleInfo> rules = widget.propRules().getValue();
            System.out.println(rules);
            assertEquals(rules.size(), 1);

            assertEquals("visible", rules.get(0).getPropID());

            // Bug used to turn "A&&B&&C&&D" into "pv0&&pv1&&pv2&&", dropping "...pv3"
            final List<ExpressionInfo<?>> expressions = rules.get(0).getExpressions();
            assertEquals(expressions.size(), 1);
            assertEquals("!(pv0&&pv1&&pv2&&pv3)", expressions.get(0).getExp());
        }
    }
}
