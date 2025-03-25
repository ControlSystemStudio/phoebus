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
public class ExecuteCommandTest {
    
    private static final String TMP_OUTFILE = "/tmp/outfile.bob";

    @Test
    public void execute_command() {
        String xml = "<display typeId=\"org.csstudio.opibuilder.Display\" version=\"1.0.0\">"
                + "<widget typeId=\"org.csstudio.opibuilder.widgets.ActionButton\" version=\"2.0.0\">"
                + "<actions hook=\"false\" hook_all=\"true\"><action type=\"EXECUTE_CMD\">"
                + "<command>echo hello</command><command_directory>$(user.home)</command_directory>"
                + "<wait_time>10</wait_time><description></description></action></actions>"
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
            assertTrue(file_content.contains("<command>echo hello</command>"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception thrown" + e.getLocalizedMessage());
        }
        // Test clean up
        File tmpfile = new File(TMP_OUTFILE); 
        tmpfile.delete();
    }
}
