package org.csstudio.display.builder.runtime.app;

import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.util.List;
import java.util.stream.Collectors;

public class ContextMenuAppendPvToClipboard implements ContextMenuEntry {
    @Override
    public String getName() {
        return org.csstudio.display.builder.runtime.Messages.AppendPVNameToClipboard;
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
    public void call(final Selection selection)
    {
        List<ProcessVariable> pvs = selection.getSelections();
        String pvNamesToAppendToClipboard = pvs.stream().map(ProcessVariable::getName).collect(Collectors.joining(System.lineSeparator()));

        Clipboard clipboard = Clipboard.getSystemClipboard();
        String newContentInClipboard;
        {
            String existingContentInClipboard;
            if (clipboard.hasString()) {
                existingContentInClipboard = clipboard.getString() + System.lineSeparator();
            } else {
                existingContentInClipboard = "";
            }
            newContentInClipboard = existingContentInClipboard + pvNamesToAppendToClipboard;
        }
        ClipboardContent newContent = new ClipboardContent();
        newContent.putString(newContentInClipboard);
        clipboard.setContent(newContent);
    }
}
