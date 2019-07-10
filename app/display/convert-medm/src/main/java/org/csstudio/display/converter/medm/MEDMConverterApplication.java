package org.csstudio.display.converter.medm;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

@SuppressWarnings("nls")
public class MEDMConverterApplication implements AppResourceDescriptor
{
    private static final List<String> FILE_EXTENSIONS = List.of("adl");
    public static final String NAME = "convert_medm";
    public static final String DISPLAY_NAME = "MEDM Converter";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public URL getIconURL()
    {
        return DisplayModel.class.getResource("/icons/display.png");
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return FILE_EXTENSIONS;
    }

    @Override
    public AppInstance create()
    {
        ExceptionDetailsErrorDialog.openError(DISPLAY_NAME, "Must be called with a file name", new Exception("No file name"));
        return null;
    }

    @Override
    public AppInstance create(final URI resource)
    {
        try
        {
            final File input = ModelResourceUtil.getFile(resource);
            final File output = new File(input.getAbsolutePath().replace(".adl", ".bob"));
            new MEDMConverter(input, output);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(DISPLAY_NAME, "Failed to open " + resource, ex);
        }
        // TODO Auto-generated method stub
        return null;
    }
}
