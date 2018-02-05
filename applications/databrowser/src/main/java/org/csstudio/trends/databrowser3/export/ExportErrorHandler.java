/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.export;

/** Interface for handler of errors during data export
 *  @author Kay Kasemir
 */
public interface ExportErrorHandler
{
    /** Will be invoked on error */
    public void handleExportError(Exception ex);
}
