/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.app.viewer3d;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;

import org.junit.Test;
import org.phoebus.applications.viewer3d.Viewer3d;
import org.phoebus.applications.viewer3d.Xform;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;

/** Demo of 3D parser.
 *
 *  Can't run as Unit test because it depends on JavaFX,
 *  i.e. won't run with cross-compilation.
 *
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class Viewer3dParserDemo
{
    /* Initialize the JavaFX platform manually. */
    static
    {
        Platform.startup(new Runnable()
        {
            @Override
            public void run()
            {
                //NOP
            }
        });
    }

    @Test
    public void buildStructure_goodInputWithComment_returnNewStructure() throws Exception
    {
        String inputWithComment = "sphere(10, 20, 30, 10.0, 150, 160, 170, 1.0, \"This is round.\")";
        Xform struct = Viewer3d.buildStructure(new ByteArrayInputStream(inputWithComment.getBytes()));
        Sphere sphere = (Sphere) struct.getChildren().get(0);

        /* Check that the transforms are correct. */

        assertEquals(10, sphere.getTranslateX(), 0);
        assertEquals(20, sphere.getTranslateY(), 0);
        assertEquals(30, sphere.getTranslateZ(), 0);

        /* Check that the size is correct. */

        assertEquals(10.0, sphere.getRadius(), 0);

        /* Check that the color is correct. */

        PhongMaterial material = (PhongMaterial) sphere.getMaterial();
        Color color = material.getDiffuseColor();

        assertEquals(150/255.0, color.getRed(), 0.001);
        assertEquals(160/255.0, color.getGreen(), 0.001);
        assertEquals(170/255.0, color.getBlue(), 0.001);
        assertEquals(1.0, color.getOpacity(), 0.0);
    }

    @Test
    public void buildStructure_goodInput_returnNewStructure() throws Exception
    {
        String input = "sphere(25, 20, 35, 108.0, 155, 24, 43, 0.565)";
        Xform struct = Viewer3d.buildStructure(new ByteArrayInputStream(input.getBytes()));
        Sphere sphere = (Sphere) struct.getChildren().get(0);

        /* Check that the transforms are correct. */

        assertEquals(25, sphere.getTranslateX(), 0);
        assertEquals(20, sphere.getTranslateY(), 0);
        assertEquals(35, sphere.getTranslateZ(), 0);

        /* Check that the size is correct. */

        assertEquals(108.0, sphere.getRadius(), 0);

        /* Check that the color is correct. */

        PhongMaterial material = (PhongMaterial) sphere.getMaterial();
        Color color = material.getDiffuseColor();

        assertEquals(155/255.0, color.getRed(), 0.001);
        assertEquals(24/255.0, color.getGreen(), 0.001);
        assertEquals(43/255.0, color.getBlue(), 0.001);
        assertEquals(0.565, color.getOpacity(), 0.001);
    }

    @Test
    public void buildStructure_badInputMissingValue_throwException()
    {
        try
        {
            /* Missing radius. */

            String input = "sphere(25, 20, 35, , 155, 24, 43, 0.565)";
            Viewer3d.buildStructure(new ByteArrayInputStream(input.getBytes()));

            /* If this is reached, the error was not caught. */

            assertTrue(false);
        }
        catch(Exception ex)
        {
            /* Exception thrown, error caught -- Success! */
        }

        try
        {
            /* Missing alpha value. */

            String input = "sphere(25, 20, 35, 22.8, 155, 24, 43,)";
            Viewer3d.buildStructure(new ByteArrayInputStream(input.getBytes()));

            /* If this is reached, the error was not caught. */

            assertTrue(false);
        }
        catch(Exception ex)
        {
            /* Exception thrown, error caught -- Success! */
        }
    }

    @Test
    public void buildStructure_badInputMalformedComment_throwException()
    {
        try
        {
            /* Malformed comment parameter. */

            String input = "sphere(25, 20, 35, 22.8, 155, 24, 43, 0.565, blah blah blah, I have no quotes!)";
            Viewer3d.buildStructure(new ByteArrayInputStream(input.getBytes()));

            /* If this is reached, the error was not caught. */

            assertTrue(false);
        }
        catch(Exception ex)
        {
            /* Exception thrown, error caught -- Success! */
        }
    }
    @Test
    public void checkAndParseComment_goodComment_returnParsedComment() throws Exception
    {
        final String comment = "\"This is a comment, to be displayed in a \\\"tooltip\\\".\"";
        final String correctComment = "This is a comment, to be displayed in a \\\"tooltip\\\".";
        final String checkedComment = Viewer3d.checkAndParseComment(comment);

        /* Check for acceptance and parsing of valid comment. */

        assertEquals(correctComment, checkedComment);
    }

    @Test
    public void checkAndParseComment_badComment_throwException()
    {
        final String commentMissingBegQuote = "This is a comment, to be displayed in a \\\"tooltip\\\".\"";
        final String commentMissingEndQuote = "\"This is a comment, to be displayed in a \\\"tooltip\\\".";

        /* Check for beginning quote. */

        try
        {
            Viewer3d.checkAndParseComment(commentMissingBegQuote);
        }
        catch(Exception ex)
        {
            assertEquals(Viewer3d.MISSING_BEG_QUOTES_ERROR, ex.getMessage());
        }

        /* Check for ending quote. */

        try
        {
            Viewer3d.checkAndParseComment(commentMissingEndQuote);
        }
        catch(Exception ex)
        {
            assertEquals(Viewer3d.MISSING_END_QUOTES_ERROR, ex.getMessage());
        }
    }
}
