package org.phoebus.applications.filebrowser;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.ui.javafx.TreeHelper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

/** JFX Tree item for a file (leaf) or directory (has children)
 *
 *  <p>Loads child nodes by traversing file system on-demand
 *  in background thread.
 *
 *  <p>Monitors folders for changes.
 *
 *  @author Kunal Shroff
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class FileTreeItem extends TreeItem<FileInfo> {

    private final DirectoryMonitor monitor;
    private AtomicBoolean isFirstTimeLeaf = new AtomicBoolean(true);
    private AtomicBoolean isFirstTimeChildren = new AtomicBoolean(true);
    private volatile boolean isLeaf;

    public FileTreeItem(final DirectoryMonitor monitor, final File childFile) {
        super(new FileInfo(childFile));
        this.monitor = monitor;
    }

    DirectoryMonitor getMonitor()
    {
        return monitor;
    }

    /** Reset so next time item is drawn, it fetches file system information */
    public void forceRefresh()
    {
        isFirstTimeLeaf.set(true);
        isFirstTimeChildren.set(true);

        TreeHelper.triggerTreeItemRefresh(this);
        setExpanded(false);
        setExpanded(true);
    }

    /** @param siblings List of FileTreeItem to sort by file name */
    static void sortSiblings(final List<TreeItem<FileInfo>> siblings)
    {
        siblings.sort((a, b) -> fileTreeItemComparator.compare(a.getValue().file, b.getValue().file));
    }

    @Override
    public ObservableList<TreeItem<FileInfo>> getChildren() {

        if (isFirstTimeChildren.getAndSet(false))
        {
            // Fetch children in background job, since file access could hang for a long time.
            // This means we return the old, i.e. empty list while the job is running.
            JobManager.schedule("Files in " + getValue().file.getName(), monitor ->
            {
                final ObservableList<TreeItem<FileInfo>> files = buildChildren(monitor, this);
                // Once job fetched files, update child items back on UI thread
                Platform.runLater(() -> super.getChildren().setAll(files));
            });
        }
        return super.getChildren();
    }

    /** Check if item refers to a file.
     *
     *  <p>Note that calling this is more effective than
     *  continuing to call getValue().isFile(),
     *  because the check is only performed once
     */
    @Override
    public boolean isLeaf()
    {
        if (isFirstTimeLeaf.getAndSet(false))
        {
            // In principle, File access could be slow.
            // But UI called isLeaf() and needs an answer _now_,
            // cannot provide the answer later from a background thread.
            // Only option would be to check with a timeout...
            // On the upside, haven't observed a hangup in here,
            // maybe because by the time we get called, this FileTreeItem
            // already exists, i.e. the File has been created.
            // If there was a hangup, it had happend in buildChildren()
            // while trying to obtain the File.
            final File f = getValue().file;
            isLeaf = f.isFile();
        }
        return isLeaf;
    }

    private ObservableList<TreeItem<FileInfo>> buildChildren(final JobMonitor job, final TreeItem<FileInfo> TreeItem) {
        final File f = TreeItem.getValue().file;
        if (f != null && f.isDirectory()) {
            final File[] files = f.listFiles();
            if (files != null) {
                Arrays.sort(files, fileTreeItemComparator);
                final ObservableList<TreeItem<FileInfo>> children = FXCollections.observableArrayList();
                job.beginTask("List " + files.length + " files");
                for (File childFile : files) {
                    if (job.isCanceled())
                        break;
                    // Keep hidden files hidden?
                    if (childFile.isHidden()  &&  !FileBrowserApp.show_hidden)
                        continue;
                    children.add(new FileTreeItem(monitor, childFile));
                }
                // Have current set of files, monitor from now on
                monitor.monitor(f);
                return children;
            }
            // No files, yet, but monitor directory to learn about additions
            monitor.monitor(f);
        }

        return FXCollections.emptyObservableList();
    }

    static Comparator<File> fileTreeItemComparator = (a, b) -> {
        if (a.isFile() != b.isFile())
            return a.isFile() ? 1 : -1;
        return a.getName().compareTo(b.getName());
    };
}