package org.phoebus.applications.probe;

import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.csstudio.display.builder.runtime.app.DisplayRuntimeInstance;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ContextMenuCopyPvToClipboardWithDescription implements ContextMenuEntry {
    @Override
    public String getName() {
        return Messages.CopyPVNameToClipboardWithDescription;
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

        String defaultDescription;
        {
            var activeDockPane = DockPane.getActiveDockPane();
            var activeDockItem = (DockItem) activeDockPane.getSelectionModel().getSelectedItem();
            if (activeDockItem.getApplication() instanceof DisplayRuntimeInstance) {
                DisplayRuntimeInstance displayRuntimeInstance = (DisplayRuntimeInstance) activeDockItem.getApplication();
                defaultDescription = displayRuntimeInstance.getDisplayName();
            }
            else {
                defaultDescription = "";
            }
        }

        BiConsumer<String, String> copyPVAndDescriptionToClipboardContinuation = (pvName, description) -> {
            String newContentInClipboard;
            {
                newContentInClipboard = pvName + "," + description;
            }
            ClipboardContent newContent = new ClipboardContent();
            newContent.putString(newContentInClipboard);
            Clipboard clipboard = Clipboard.getSystemClipboard();
            clipboard.setContent(newContent);
        };

        String pvName = pvNamesToAppendToClipboard;

        ContextMenuAppendPvToClipboardWithDescription.addDescriptionToPvNameModalDialog(pvName,
                                                                                        defaultDescription,
                                                                                        "Copy",
                                                                                        copyPVAndDescriptionToClipboardContinuation);
    }
}
