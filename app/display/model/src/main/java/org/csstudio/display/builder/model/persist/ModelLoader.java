/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.persist;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.io.InputStream;
import java.util.ServiceLoader;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.WidgetClassSupport;
import org.csstudio.display.builder.model.spi.DisplayAutoConverter;
import org.csstudio.display.builder.model.util.ModelResourceUtil;

/** Helper for loading a display model
 *
 *  <p>Resolves display path relative to parent display,
 *  then loads the model,
 *  updates the model's input file information
 *  and applies the class definitions (except for *.bcf files).
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ModelLoader
{
    /** Load model, resolved relative to parent, with classes applied (except for *.bcf itself)
     *
     *  <p>Selects *.bob over *.opi.
     *
     *  @param parent_display Path to a 'parent' file, may be <code>null</code>
     *  @param display_file Model file
     *  @return {@link DisplayModel}
     *  @throws Exception on error
     */
    public static DisplayModel resolveAndLoadModel(final String parent_display, final String display_file) throws Exception
    {
        try
        {
            final String resolved_name = ModelResourceUtil.resolveResource(parent_display, display_file);
            return loadModel(resolved_name);
        }
        catch (Exception ex)
        {
            try
            {
                // Check for auto-converters before giving up
                for (DisplayAutoConverter converter : ServiceLoader.load(DisplayAutoConverter.class))
                {
                    final DisplayModel converted = converter.autoconvert(parent_display, display_file);
                    if (converted != null)
                        return converted;
                }
            }
            catch (Exception conversion_error)
            {
                logger.log(Level.WARNING, "Auto-converter failed to create " + display_file, conversion_error);
            }
            throw new Exception("Cannot load '" + display_file + "' (parent: '" + parent_display + "')", ex);
        }
    }

    /** Load model, with classes applied (except for *.bcf itself)
     *
     *  @param display_file Model file
     *  @return {@link DisplayModel}
     *  @throws Exception on error
     */
    public static DisplayModel loadModel(final String display_file) throws Exception
    {
        return loadModel(ModelResourceUtil.openResourceStream(display_file), display_file);
    }


    /** Load model, with classes applied (except for *.bcf itself)
     *
     *  @param stream Stream for the display
     *  @param display_file Model file path, will be registered via {@link DisplayModel#USER_DATA_INPUT_FILE}
     *  @return {@link DisplayModel}
     *  @throws Exception on error
     */
    public static DisplayModel loadModel(final InputStream stream, final String display_file) throws Exception
    {
        final ModelReader reader = new ModelReader(stream, display_file);
        final DisplayModel model = reader.readModel();
        model.setUserData(DisplayModel.USER_DATA_INPUT_FILE, display_file);

        // Models from version 2 on support classes
        if (reader.getVersion().getMajor() >= 2  &&
            !display_file.endsWith(WidgetClassSupport.FILE_EXTENSION))
        {
            WidgetClassesService.getWidgetClasses().apply(model);
        }
        return model;
  }
}
