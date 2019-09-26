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
import org.csstudio.opibuilder.converter.model.Edm_activePngClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activePngClass extends ConverterBase<PictureWidget>
{
    public Convert_activePngClass(final EdmConverter converter, final Widget parent, final Edm_activePngClass r)
    {
        super(converter, parent, r);

        if (r.getFile() != null)
        {
            // Does EDM only support *.png?
            String file = r.getFile();
            if (!file.toLowerCase().endsWith(".png"))
                file = file + ".png";
            widget.propFile().setValue(file);

            try
            {
                converter.downloadAsset(file);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "PNG image cannot get " + file, ex);
            }
        }
    }

    @Override
    protected PictureWidget createWidget(final EdmWidget edm)
    {
        return new PictureWidget();
    }
}
