package org.phoebus.applications.display.navigation;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.actions.OpenDisplayAction;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroOrSystemProvider;
import org.phoebus.framework.macros.Macros;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A utility Class for handling the navigation of .bob and .opi files
 */
public class ProcessOPI {
    private static final Logger logger = Logger.getLogger(ProcessOPI.class.getName());

    private final File rootFile;
    private final Set<File> allLinkedFiles;

    /**
     * @param rootFile Start of the navigation tree
     */
    public ProcessOPI(File rootFile) {
        this.rootFile = rootFile;
        this.allLinkedFiles = new HashSet<>();
    }

    /**
     * Gets All the files linked to via the rootFile
     * This call should be made on a separate thread since it may take some time to process all the linked files
     */
    public Set<File> process() {
        getExtensionByStringHandling(this.rootFile.getName()).ifPresentOrElse(ext -> {
            if (!ext.equalsIgnoreCase("bob") && !ext.equalsIgnoreCase("opi")) {
                throw new UnsupportedOperationException("File extension " + ext + " is not supported. The supported extensions are .bob and .opi.");
            }
        }, () -> {
            throw new UnsupportedOperationException("File extension unknown");
        });
        logger.log(Level.INFO, "Processing file : " + this.rootFile);
        getAllLinkedFiles(this.rootFile);
        return this.allLinkedFiles;
    }

    private synchronized void getAllLinkedFiles(File file) {
        logger.log(Level.INFO, "Calculating linked files for " + file.getName());
        Set<File> linkedFiles = getLinkedFiles(file);
        linkedFiles.forEach(f -> {
            if (allLinkedFiles.contains(f) || f.equals(rootFile)) {
                // Already handled skip it
            } else {
                // Find all the linked files for this file
                allLinkedFiles.add(f);
                getAllLinkedFiles(f);
            }
        });
    }

    /**
     * A Utility method which creates a list of all the files that can be launched from a given OPI.
     * It only inlcudes files launched via actions.
     *
     * @param file root OPI file
     * @return a unique Set of all files that can be reached from root OPI file
     */
    public static synchronized Set<File> getLinkedFiles(File file) {
        Set<File> result = new HashSet<>();
        try {
            ModelReader reader = new ModelReader(new FileInputStream(file));
            DisplayModel model = reader.readModel();
            List<Widget> children = model.getChildren();
            List<ActionInfo> actionsInfos = new ArrayList<>();
            children.forEach(widget -> {
                // Find all the action properties
                WidgetProperty<ActionInfos> actions = widget.propActions();
                Set<ActionInfo> openActions = actions.getValue().getActions().stream().filter(actionInfo -> actionInfo.getType().equalsIgnoreCase("open_display")).collect(Collectors.toSet());

                // Resolve the complete valid path for each of the open display actions
                openActions.forEach(openAction -> {
                    // Path to resolve, after expanding macros of source widget and action
                    OpenDisplayAction action = (OpenDisplayAction) openAction;
                    try {
                        // Path to resolve, after expanding macros of action in environment of source widget
                        final Macros macros = action.getMacros(); // Not copying, just using action's macros
                        macros.expandValues(widget.getEffectiveMacros());

                        // For display path, use the combined macros...
                        String expanded_path = MacroHandler.replace(new MacroOrSystemProvider(macros), action.getFile());
                        // .. but fall back to properties
                        expanded_path = MacroHandler.replace(widget.getMacrosOrProperties(), expanded_path);

                        String resource = ModelResourceUtil.resolveResource(file.getPath(), expanded_path);
                        result.add(new File(resource));
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to resolve macros for : " + action.getFile(), e);
                    }
                });

                actionsInfos.addAll(openActions);
            });
            return result;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to getLinkedFiles for file " + file.getPath(), e);
        }
        return result;
    }

    private Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }
}
