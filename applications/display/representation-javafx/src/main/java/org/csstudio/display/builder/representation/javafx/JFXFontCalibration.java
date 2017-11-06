/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.display.builder.representation.FontCalibration;

import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/** Java FX Font calibration
 *
 *  <p>Can also be executed as demo
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JFXFontCalibration extends Application implements FontCalibration
{
    private final Logger logger = Logger.getLogger(getClass().getName());
    private Text text = new Text(FontCalibration.TEXT);

    @Override
    public double getCalibrationFactor() throws Exception
    {
        final Font font = Font.font(FontCalibration.FONT, FontCalibration.SIZE);
        if (! font.getName().startsWith(FontCalibration.FONT))
        {
            logger.log(Level.SEVERE, "Cannot obtain font " + FontCalibration.FONT + " for calibration. Got " + font.getName());
            logger.log(Level.SEVERE, "Font calibration will default to 1.0. Check installation of calibration font");
            return 1.0;
        }
        text.setFont(font);

        final Bounds measure = text.getLayoutBounds();
        logger.log(Level.FINE,
                   "Font calibration measure: " + measure.getWidth() + " x " + measure.getHeight());
        final double factor = FontCalibration.PIXEL_WIDTH / measure.getWidth();
        logger.log(Level.CONFIG, "JFX font calibration factor: {0}", factor);
        return factor;
    }

    public static void main(final String[] args)
    {
        launch(args);
    }

    @Override
    public void start(final Stage stage) throws Exception
    {
        Logger.getLogger("").setLevel(Level.FINE);
        for (Handler handler : Logger.getLogger("").getHandlers())
            handler.setLevel(Level.FINE);

        final double factor = getCalibrationFactor();

        final BorderPane root = new BorderPane(text);
        final Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Java FX: Calibration factor " + factor);

        if (Math.abs(factor - 1.0) > 0.01)
           System.err.println("Calibration is not 1.0 but " + factor);

        stage.show();
    }
}