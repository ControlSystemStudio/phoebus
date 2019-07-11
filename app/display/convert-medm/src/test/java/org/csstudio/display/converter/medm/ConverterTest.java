package org.csstudio.display.converter.medm;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("nls")
public class ConverterTest
{
    @Before
    public void init()
    {
        final Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        for (Handler handler : root.getHandlers())
            handler.setLevel(root.getLevel());
    }

    @Test
    public void testConverter() throws Exception
    {
        final String filename = ConverterTest.class.getResource("/Main_XXXX.adl").getFile();
        if (filename.isEmpty())
            throw new Exception("Cannot obtain test file");
        new MEDMConverter(new File(filename), null);
    }
}
