package org.phoebus.apps.trends.rich.context;

import org.phoebus.framework.selection.Selection;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.scene.image.Image;

/**
 * 
 * @author Kunal Shroff
 */
public class RichEmailContextMenu implements ContextMenuEntry{
    private static final Image icon = ImageCache.getImage(ImageCache.class, "/icons/mail-send-16.png");

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<?> getSupportedType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void call(Selection selection) throws Exception {
        // TODO Auto-generated method stub
        
    }

}
