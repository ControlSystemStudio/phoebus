package org.phoebus.applications.display.navigation;

import javafx.concurrent.Task;

import java.io.File;
import java.util.Set;

/**
 * For the given opi resource the services find ALL linked files.
 */
public class ProcessOPIAllLinksTask extends Task<Set<File>> {

    private final File rootFile;
    private final ProcessOPI processOPI;

    public ProcessOPIAllLinksTask(File rootFile) {
        this.rootFile = rootFile;
        processOPI = new ProcessOPI(rootFile);
    }

    @Override
    protected Set<File> call() throws Exception {
        return processOPI.process();
    }
}
