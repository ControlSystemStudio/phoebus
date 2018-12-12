/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import java.util.Objects;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import javafx.application.Platform;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
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

    /** @param node Node that will be printed */
    public PrintAction(final Node node)
    {
        super(Messages.Print, new ImageView(icon));
        setOnAction(event -> Platform.runLater(() -> print(node)));
    }

    private void print(final Node node)
    {
        try
        {
            // Select printer
            final PrinterJob job = Objects.requireNonNull(PrinterJob.createPrinterJob(), "Cannot create printer job");
            final Scene scene = Objects.requireNonNull(node.getScene(), "Missing Scene");

            if (! job.showPrintDialog(scene.getWindow()))
                return;

            // Get Screenshot
            final WritableImage screenshot = node.snapshot(null, null);

            // Scale image to full page
            final Printer printer = job.getPrinter();
            final Paper paper = job.getJobSettings().getPageLayout().getPaper();
            final PageLayout pageLayout = printer.createPageLayout(paper,
                                                                   PageOrientation.LANDSCAPE,
                                                                   Printer.MarginType.DEFAULT);
            final double scaleX = pageLayout.getPrintableWidth() / screenshot.getWidth();
            final double scaleY = pageLayout.getPrintableHeight() / screenshot.getHeight();
            final double scale = Math.min(scaleX, scaleY);
            final ImageView print_node = new ImageView(screenshot);
            print_node.getTransforms().add(new Scale(scale, scale));

            // Print off the UI thread
            JobManager.schedule(Messages.Print, monitor ->
            {
                if (job.printPage(print_node))
                    job.endJob();
            });
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(node, Messages.Print, Messages.PrintErr, ex);
        }
    }
}
