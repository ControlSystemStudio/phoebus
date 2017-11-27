/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import org.csstudio.display.builder.runtime.Messages;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.ImageCache;

import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Scale;

/** Action for printing snapshot of display
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PrintAction extends MenuItem
{
    private static final Image icon = ImageCache.getImage(PrintAction.class, "/icons/print_edit.png");

    public PrintAction(final Parent model_parent)
    {
        super(Messages.Print, new ImageView(icon));
        setOnAction(event -> print(model_parent));
    }

    private void print(final Parent model_parent)
    {
        // Select printer
        final PrinterJob job = PrinterJob.createPrinterJob();
        if (! job.showPrintDialog(model_parent.getScene().getWindow()))
            return;

        // Get Screenshot
        final WritableImage screenshot = model_parent.snapshot(null, null);

        // Scale image to full page
        final Printer printer = job.getPrinter();
        final PageLayout pageLayout = printer.createPageLayout(Paper.NA_LETTER,
                                                               PageOrientation.LANDSCAPE,
                                                               Printer.MarginType.DEFAULT);
        final double scaleX = pageLayout.getPrintableWidth() / screenshot.getWidth();
        final double scaleY = pageLayout.getPrintableHeight() / screenshot.getHeight();
        final double scale = Math.min(scaleX, scaleY);
        final ImageView print_node = new ImageView(screenshot);
        print_node.getTransforms().add(new Scale(scale, scale));

        // Print off the UI thread
        JobManager.schedule(Messages.SaveSnapshot, monitor ->
        {
            if (job.printPage(print_node))
                job.endJob();
        });
    }
}
