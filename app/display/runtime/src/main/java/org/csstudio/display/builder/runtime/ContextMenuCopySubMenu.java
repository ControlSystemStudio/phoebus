package org.csstudio.display.builder.runtime;

import javafx.scene.image.Image;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.util.Arrays;
import java.util.List;

public class ContextMenuCopySubMenu implements ContextMenuEntry {
    @Override
    public String getName() {
        return Messages.CopySubMenu;
    }

    private Image icon = ImageCache.getImage(ImageCache.class, "/icons/copy.png");
    @Override
    public Image getIcon() {
        return icon;
    }

    @Override
    public Class<?> getSupportedType() {
        return ProcessVariable.class;
    }

    @Override
    public List<ContextMenuEntry> getChildren() {
        return Arrays.asList(new ContextMenuCopyPvToClipboard(),
                             new ContextMenuCopyPvToClipboardWithDescription(),
                             new ContextMenuAppendPvToClipboard(),
                             new ContextMenuAppendPvToClipboardWithDescription(),
                             new ContextMenuCopyPvAndValueToClipboard());
    }
}
