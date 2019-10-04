/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.PictureWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_cfcf6c8a_dbeb_11d2_8a97_00104b8742df;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
// cf... is the EDM class name for its gif widget
@SuppressWarnings("nls")
public class Convert_cfcf6c8a_dbeb_11d2_8a97_00104b8742df extends ConverterBase<PictureWidget>
{
    public Convert_cfcf6c8a_dbeb_11d2_8a97_00104b8742df(final EdmConverter converter, final Widget parent, final Edm_cfcf6c8a_dbeb_11d2_8a97_00104b8742df r)
    {
        super(converter, parent, r);

        final String file = r.getFile();
        if (file != null)
        {
            widget.propFile().setValue(file);
            try
            {
                converter.downloadAsset(file);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "GIF image cannot get " + file, ex);
            }
        }
    }

    @Override
    protected PictureWidget createWidget(final EdmWidget edm)
    {
        return new PictureWidget();
    }
}
