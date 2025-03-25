/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */
package org.csstudio.display.actions;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.junit.jupiter.api.Test;

/**
 * Test Execute Script actions
 * 
 * @author Becky Auger-Williams
 */
public class ExecuteScriptTest {
    
    private static final String TMP_OUTFILE = "/tmp/outfile.bob";

    @Test
    public void execute_embedded_javascript() {
        String xml = "<display typeId=\"org.csstudio.opibuilder.Display\" version=\"1.0.0\">"
                + "<widget typeId=\"org.csstudio.opibuilder.widgets.ActionButton\" version=\"2.0.0\">"
                + "<actions hook=\"false\" hook_all=\"true\"><action type=\"EXECUTE_JAVASCRIPT\"><path></path>"
                + "<scriptText><![CDATA[importPackage(Packages.org.csstudio.opibuilder.scriptUtil);]]></scriptText>"
                + "<embedded>true</embedded><description></description></action></actions>"
                + "</widget></display>";

        InputStream stream = new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8")));
        try (FileOutputStream outStream = new FileOutputStream(TMP_OUTFILE);
             ModelWriter writer = new ModelWriter(outStream);) {

            ModelReader reader = new ModelReader(stream);
            DisplayModel model = reader.readModel();
            assertTrue(model.isClean());

            writer.writeModel(model);

            // Check the model gets written correctly
            String file_content = Files.readString(Path.of(TMP_OUTFILE)).strip();
            assertTrue(file_content.contains("<text><![CDATA[importPackage(Packages.org.csstudio.opibuilder.scriptUtil);]]></text>"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown" + e.getLocalizedMessage());
        }
        // Test clean up
        File tmpfile = new File(TMP_OUTFILE); 
        tmpfile.delete();
    }
    
    @Test
    public void execute_embedded_pythonscript() {
        String xml = "<display typeId=\"org.csstudio.opibuilder.Display\" version=\"1.0.0\">"
                + "<widget typeId=\"org.csstudio.opibuilder.widgets.ActionButton\" version=\"2.0.0\">"
                + "<actions hook=\"false\" hook_all=\"true\"><action type=\"EXECUTE_PYTHONSCRIPT\"><path></path>"
                + "<scriptText><![CDATA[from org.csstudio.opibuilder.scriptUtil import PVUtil\n"
                + "]]></scriptText>"
                + "<embedded>true</embedded><description></description></action></actions>"
                + "</widget></display>";

        InputStream stream = new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8")));
        try (FileOutputStream outStream = new FileOutputStream(TMP_OUTFILE);
             ModelWriter writer = new ModelWriter(outStream);) {

            ModelReader reader = new ModelReader(stream);
            DisplayModel model = reader.readModel();
            assertTrue(model.isClean());

            writer.writeModel(model);
            
            // Check the model gets written correctly
            String file_content = Files.readString(Path.of(TMP_OUTFILE)).strip();
            assertTrue(file_content.contains("<text><![CDATA[from org.csstudio.opibuilder.scriptUtil import PVUtil\n"
                    + "]]></text>"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown" + e.getLocalizedMessage());
        }
        // Test clean up
        File tmpfile = new File(TMP_OUTFILE); 
        tmpfile.delete();
    }
}
