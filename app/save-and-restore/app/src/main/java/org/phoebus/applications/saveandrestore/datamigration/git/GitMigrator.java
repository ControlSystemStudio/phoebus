/*
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.applications.saveandrestore.datamigration.git;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.preferences.PreferencesReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Migrates configurations and snapshots from a (cloned) git repository. The migration process makes some assumptions with
 * regards to the file and directory structure of the repository:
 * <ul>
 *     <li>Configuration (aka beamline set) files - named *.bms - are found in directories named BeamlineSets.</li>
 *     <li>Snapshots files - named *.snp - are found in directories Snapshots.</li>
 *     <li>There is a one-to-one relation between a bms file and a snp file. The snp file must exist in the Snapshots
 *      directory having same parent as the BeamlineSets directory where the bms file is found.</li>
 *     <li>The git repository is  processed under the assumption that BeamlineSets and
 *      Snapshots directories may define a structure of directories (e.g. describing a sub-system structure), but
 *      may not contain any additional BeamlineSets or Snapshots directories at any level.</li>
 * </ul>
 * <p>
 * The migration will create a top-level folder in the save-and-restore tree named "Migration &lt;current date and
 * time&gt;" under which the migration data will be created.
 * <p>
 * To run the migration, build the product as usual and invoke like so:
 * java -jar /path/to/phoebus/product-launcher.jar -settings myPhoebusIniFile -main org.phoebus.applications.saveandrestore.datamigration.git.GitMigrator /path/to/git/working/directory
 * <p>
 * The settings (ini) file must specify the URL for the save-and-restore service like so:
 * org.phoebus.applications.saveandrestore.datamigration.git/jmasar.service.url=&lt;saveAndRestoreServiceUrl&gt;
 * <p>
 * Add the following setting to the setting file to map Git tag to {@link Tag} instead of Golden tag.
 * org.phoebus.applications.saveandrestore.datamigration.git/useMultipleTag=true
 * <p>
 * Add the following setting to the setting file to keep savesets with no snapshot created from them.
 * org.phoebus.applications.saveandrestore.datamigration.git/keepSavesetWithNoSnapshot=true
 * <p>
 * Add the following setting to the setting file to ignore duplicate snapshots (same commit time)
 * org.phoebus.applications.saveandrestore.datamigration.git/ignoreDuplicateSnapshots=true
 * <p>
 * Add the following setting to the setting file to add PVs for incompatible configuration to correctly migrate snapshots
 * with the newer version of saveset modified by user after older snapshots are taken with older version of saveset
 * org.phoebus.applications.saveandrestore.datamigration.git/addPVsForIncompatibleSaveset=true
 */
public class GitMigrator {

    private final SaveAndRestoreService saveAndRestoreService;

    private final Boolean useMultipleTag;

    private final Boolean keepSavesetWithNoSnapshot;

    private final Boolean ignoreDuplicateSnapshots;

    private final Boolean addPVsForIncompatibleSaveset;

    private Git git;
    private File gitRoot;
    private Node migrationRootNode;

    public GitMigrator() {
        saveAndRestoreService = SaveAndRestoreService.getInstance();
        PreferencesReader preferencesReader = new PreferencesReader(SaveAndRestoreApplication.class, "/save_and_restore_preferences.properties");
        useMultipleTag = preferencesReader.getBoolean("useMultipleTag");
        keepSavesetWithNoSnapshot = preferencesReader.getBoolean("keepSavesetWithNoSnapshot");
        ignoreDuplicateSnapshots = preferencesReader.getBoolean("ignoreDuplicateSnapshots");
        addPVsForIncompatibleSaveset = preferencesReader.getBoolean("addPVsForIncompatibleSaveset");
    }

    public void run(String... args) {

        gitRoot = new File(args[0]);
        if (!gitRoot.exists() || !gitRoot.isDirectory()) {
            System.out.println("Git working directory " + gitRoot.getAbsolutePath() + " does not exist or is not a directory");
            System.exit(0);
        }

        try {
            git = Git.open(new File(gitRoot, "/.git"));
        } catch (IOException e) {
            System.out.println("Unable to create local Git object, cause: " + e.getMessage());
            System.exit(0);
        }


        // Create a date stamoed migration root node
        Node rootNode = saveAndRestoreService.getRootNode();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            migrationRootNode = saveAndRestoreService.createNode(rootNode.getUniqueId(), Node.builder()
                    .name("Migration " + simpleDateFormat.format(new Date()))
                    .userName("Save-and-restore migrator")
                    .build());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        List<FilePair> bmsFiles = new ArrayList<>();
        bmsFiles = findBeamlineSetFiles(bmsFiles, gitRoot);

        bmsFiles.forEach(f -> {
            try {
                createSaveSetAndAssociatedSnapshots(f);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        System.exit(0);
    }

    /**
     * Recursively find bms files
     *
     * @param beamlineSetFiles
     * @param directory
     * @return
     */
    private List<FilePair> findBeamlineSetFiles(List<FilePair> beamlineSetFiles, File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                findBeamlineSetFiles(beamlineSetFiles, file);
            } else if (file.getName().endsWith("bms")) {
                String snapshotFile = findSnapshotFile(file.getAbsolutePath());

                if (snapshotFile == null && !keepSavesetWithNoSnapshot) {
                    continue;
                }

                FilePair filePair = new FilePair(file.getAbsolutePath(), snapshotFile);
                beamlineSetFiles.add(filePair);
            }
        }
        return beamlineSetFiles;
    }

    private String findSnapshotFile(String beamlineSetFile) {
        String expectedPath =
                beamlineSetFile.replaceAll("BeamlineSets", "Snapshots").replace("bms", "snp");
        if (new File(expectedPath).exists()) {
            return expectedPath;
        }
        return null;
    }


    /**
     * Holds a pair of strings representing a bms file and an
     * associated snp file. The strings are file paths relative to
     * the git root specified on command line.
     */
    private class FilePair {
        public String bms;
        public String snp;

        public FilePair(String bms, String snp) {
            this.bms = bms.substring(gitRoot.getAbsolutePath().length() + 1);

            if (snp == null) {
                this.snp = "";
            } else {
                this.snp = snp.substring(gitRoot.getAbsolutePath().length() + 1);
            }
        }

        @Override
        public String toString() {
            return bms + " " + snp;
        }
    }

    private void createSaveSetAndAssociatedSnapshots(FilePair filePair) throws Exception {
        // Remove all occurrences of /BeamlineSets
        String path = filePair.bms.replaceAll("/BeamlineSets", "");
        // Tokenize
        String[] pathElements = path.split("/");

        Node parentNode = createFolderStructure(pathElements, 0, migrationRootNode);
        createSaveSetNode(parentNode, filePair);
    }

    private Node createFolderStructure(String[] pathElements, int level, Node parentNode) throws Exception {
        String path = pathElements[level];
        if (path.endsWith("bms")) {
            return parentNode;
        }

        List<Node> childNodes = saveAndRestoreService.getChildNodes(parentNode);
        for (Node childNode : childNodes) {
            if (childNode.getNodeType().equals(NodeType.FOLDER) &&
                    childNode.getName().equals(path)) {
                return createFolderStructure(pathElements, level + 1, childNode);
            }
        }

        Node nextFolderNode = saveAndRestoreService.createNode(parentNode.getUniqueId(),
                Node.builder()
                        .name(path)
                        .nodeType(NodeType.FOLDER)
                        .userName(System.getProperty("user.name"))
                        .created(new Date())
                        .lastModified(new Date())
                        .build());

        return createFolderStructure(pathElements, level + 1, nextFolderNode);
    }

    private Node createSaveSetNode(Node parentNode, FilePair filePair) {
        Node saveSetNode;
        Configuration configuration = new Configuration();
        try {
            List<RevCommit> commits = findCommitsFor(filePair.bms);
            // Only latest commit considered for bms files
            RevCommit commit = commits.get(0);
            String author = commit.getAuthorIdent().getName();
            String commitMessage = commit.getFullMessage();
            Date commitDate = new Date(commit.getCommitTime() * 1000L);
            saveSetNode = Node.builder()
                    .name(filePair.bms.substring(filePair.bms.lastIndexOf("/") + 1).replace(".bms", ""))
                    .nodeType(NodeType.CONFIGURATION)
                    .userName(author)
                    .created(commitDate)
                    .lastModified(commitDate)
                    .description(commitMessage)
                    .build();

            String fullPath = gitRoot.getAbsolutePath() + "/" + filePair.bms;
            List<ConfigPv> configPvs = FileReaderHelper.readSaveSet(new FileInputStream(fullPath));
            ConfigurationData configurationData = new ConfigurationData();
            configurationData.setPvList(configPvs);

            configuration.setConfigurationNode(saveSetNode);
            configuration.setConfigurationData(configurationData);

            configuration = saveAndRestoreService.createConfiguration(parentNode, configuration);
            saveSetNode = configuration.getConfigurationNode();
            if (!filePair.snp.isEmpty()) {
                createSnapshots(saveSetNode, filePair.snp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return configuration.getConfigurationNode();
    }

    private void createSnapshots(Node saveSetNode, String relativeSnpFilePath) {
        try {
            List<RevCommit> commits = findCommitsFor(relativeSnpFilePath);
            Map<String, RevTag> tags = loadTagsForRevisions(commits);
            for (RevCommit commit : commits) {
                try (ObjectReader objectReader = git.getRepository().newObjectReader(); TreeWalk treeWalk = new TreeWalk(objectReader)) {
                    CanonicalTreeParser treeParser = new CanonicalTreeParser();
                    treeParser.reset(objectReader, commit.getTree());
                    int treeIndex = treeWalk.addTree(treeParser);
                    treeWalk.setFilter(PathFilter.create(relativeSnpFilePath));
                    treeWalk.setRecursive(true);
                    if (treeWalk.next()) {
                        AbstractTreeIterator iterator = treeWalk.getTree(treeIndex, AbstractTreeIterator.class);
                        ObjectId objectId = iterator.getEntryObjectId();
                        ObjectLoader objectLoader = objectReader.open(objectId);
                        RevTag tag = tags.get(commit.getName());
                        try (InputStream stream = objectLoader.openStream()) {
                            List<SnapshotItem> snapshotItems = FileReaderHelper.readSnapshot(stream);

                            Date commitTime = new Date(commit.getCommitTime() * 1000L);
                            String snapshotName = commitTime.toString();

                            if (!isSnapshotCompatibleWithSaveSet(saveSetNode, snapshotItems)) {
                                System.out.println("------------------------------------------------------------------------------------");
                                System.out.println(" Snapshot not compatible with the saveset!");
                                System.out.println(" Check if PV names are the same in saveset and snapshot!");
                                System.out.println("------------------------------------------------------------------------------------");
                                System.out.println("    Commit: " + commit.getName());
                                System.out.println("   Configuration: " + DirectoryUtilities.CreateLocationString(saveSetNode, false));
                                System.out.println(" Timestamp: " + snapshotName);
                                System.out.println("------------------------------------------------------------------------------------");

                                continue;
                            }
                            snapshotItems = setConfigPvIds(saveSetNode, snapshotItems);

                            List<Node> nodeList = saveAndRestoreService.getChildNodes(saveSetNode);
                            final String snapshotNameForChecking = snapshotName;
                            boolean isDuplicateSnapshotName = nodeList.stream().anyMatch(item -> item.getName().equals(snapshotNameForChecking));
                            if (isDuplicateSnapshotName) {
                                if (ignoreDuplicateSnapshots) {
                                    continue;
                                }

                                String newSnapshotName = String.format("%s (%s)", snapshotName, commit.getName().substring(0, 6));

                                System.out.println("------------------------------------------------------------------------------------");
                                System.out.println(" Duplicate snapshot found!");
                                System.out.println("------------------------------------------------------------------------------------");
                                System.out.println("   Commit: " + commit.getName());
                                System.out.println("  Configuration: " + DirectoryUtilities.CreateLocationString(saveSetNode, false));
                                System.out.println(" SnapshotData: " + snapshotName);
                                System.out.println(" New name: " + newSnapshotName);
                                System.out.println("------------------------------------------------------------------------------------");

                                snapshotName = newSnapshotName;
                            }

                            Snapshot snapshot = new Snapshot();
                            snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name(snapshotName).description(commit.getFullMessage()).build());
                            SnapshotData snapshotData = new SnapshotData();
                            snapshotData.setSnasphotItems(snapshotItems);
                            snapshot.setSnapshotData(snapshotData);

                            Node snapshotNode = saveAndRestoreService.saveSnapshot(saveSetNode, snapshot).getSnapshotNode();

                            snapshotNode = saveAndRestoreService.getNode(snapshotNode.getUniqueId());
                            if (tag != null) {
                                if (useMultipleTag) {
                                    String fullTagName = tag.getTagName();
                                    String[] fullTagNameSplit = fullTagName.split("/");
                                    String tagName = fullTagNameSplit[fullTagNameSplit.length - 1];
                                    tagName = tagName.substring(1, tagName.length() - 1);

                                    Tag snapshotTag = Tag.builder()
                                            .name(tagName)
                                            .comment(tag.getFullMessage())
                                            .userName(tag.getTaggerIdent().getName())
                                            .created(tag.getTaggerIdent().getWhen())
                                            .build();

                                    snapshotNode.addTag(snapshotTag);
                                } else {
                                    snapshotNode.addTag(Tag.goldenTag(commit.getCommitterIdent().getName()));
                                }
                            }
                            snapshotNode.setUserName(commit.getCommitterIdent().getName());
                            snapshotNode.setCreated(commitTime);
                            saveAndRestoreService.updateNode(snapshotNode, true);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<RevCommit> findCommitsFor(String filePath) throws Exception {
        List<RevCommit> commitsList = new ArrayList<>();
        ObjectId obj = git.getRepository().resolve(Constants.HEAD);
        LogCommand log = git.log().add(obj).addPath(filePath);
        Iterable<RevCommit> commits = log.call();

        for (RevCommit commit : commits) {
            commitsList.add(commit);
        }

        return commitsList;
    }

    private Map<String, RevTag> loadTagsForRevisions(List<RevCommit> revisions) throws Exception {
        Map<String, Ref> tags = git.getRepository().getTags();
        Map<String, RevTag> ret = new HashMap<>();
        try (RevWalk walk = new RevWalk(git.getRepository())) {
            for (RevCommit rev : revisions) {
                String s = Git.wrap(git.getRepository()).describe().setTarget(rev).call();
                Ref tt = tags.get(s);
                if (tt != null) {
                    RevTag t = walk.parseTag(tt.getObjectId());
                    ret.put(rev.getName(), t);
                }
            }
        }
        return ret;
    }

    private List<SnapshotItem> setConfigPvIds(Node saveSetNode, List<SnapshotItem> snapshotItems) {
        List<ConfigPv> configPvs = saveAndRestoreService.getConfiguration(saveSetNode.getUniqueId()).getPvList();
        snapshotItems.forEach(snapshotItem -> configPvs.stream().filter(configPv -> configPv.equals(snapshotItem.getConfigPv())).findFirst().ifPresent(snapshotItem::setConfigPv));

        return snapshotItems;
    }

    private boolean isSnapshotCompatibleWithSaveSet(Node saveSetNode, List<SnapshotItem> snapshotItems) throws Exception {
        List<ConfigPv> configPvs = saveAndRestoreService.getConfiguration(saveSetNode.getUniqueId()).getPvList();

        if (addPVsForIncompatibleSaveset) {
            List<ConfigPv> newConfigPvs = new ArrayList<>();

            snapshotItems.forEach(snapshotItem -> {
                if (configPvs.stream().noneMatch(configPv -> configPv.equals(snapshotItem.getConfigPv()))) {
                    newConfigPvs.add(snapshotItem.getConfigPv());
                }
            });

            if (!newConfigPvs.isEmpty()) {
                ConfigurationData configurationData = saveAndRestoreService.getConfiguration(saveSetNode.getUniqueId());
                configurationData.getPvList().addAll(newConfigPvs);
                Configuration configuration = new Configuration();
                configuration.setConfigurationData(configurationData);
                configuration.setConfigurationNode(saveSetNode);
                saveAndRestoreService.updateConfiguration(configuration);
            }

            return true;
        } else {
            int compatibilityCount = 0;
            for (SnapshotItem snapshotItem : snapshotItems) {
                for (ConfigPv configPv : configPvs) {
                    if (configPv.equals(snapshotItem.getConfigPv())) {
                        compatibilityCount++;
                        break;
                    }
                }
            }
            return compatibilityCount == snapshotItems.size();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar /path/to/phoebus/product-launcher.jar -settings <myPhoebusIniFile> -main org.phoebus.applications.saveandrestore.datamigration.git.GitMigrator /path/to/git/working/directory");
            System.out.println("The ini file must specify org.phoebus.applications.saveandrestore.datamigration.git/jmasar.service.url=<saveAndRestoreServiceUrl>");
            System.exit(0);
        }

        new GitMigrator().run(args);
    }
}
