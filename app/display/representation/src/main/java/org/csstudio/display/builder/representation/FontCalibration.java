/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation;

/** Font Calibration
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public interface FontCalibration
{
    /** Font used for calibration.
     *  Open-source Courier-type monospaced font that is available
     *  from https://fedorahosted.org/liberation-fonts
     */
    public static final String FONT = "Liberation Mono";

    /** Font size ('points') used for calibration */
    public static final int SIZE = 40;

    /** Text used to calibrate font sizes */
    public static final String TEXT = "'Example' Test \"XOXO\" pq__ 1234567890";

    /** Desired size of example text.
     *
     *  <p>During initial tests, this size was consistently achieved by JavaFX
     *  without any scaling on Linux, Windows, Mac OSX
     *  because it effectively uses 1 point == 1 pixel.
     *
     *  <p>SWT on Mac OS X also used 1 point == 1 pixel,
     *  while Linux and Windows resulted pixel sizes based
     *  on the screen resolution and size, i.e. dots per inch DPI.
     */
    public static final double PIXEL_WIDTH = 888.14453125;

    /** Determine font calibration factor.
     *
     *  <p>When multiplying a nominal font size with this factor,
     *  the result should render to match the PIXEL_WIDTH.
     *
     *  <p>For JavaFX, this should always be very close to 1.0.
     *
     *  <p>For SWT on Mac OS X, this should also be 1.0.
     *  For Windows and Linux, result depends on screen DPI.
     *
     *  @return Calibration factor
     *  @throws Exception on error
     */
    public double getCalibrationFactor() throws Exception;
}
