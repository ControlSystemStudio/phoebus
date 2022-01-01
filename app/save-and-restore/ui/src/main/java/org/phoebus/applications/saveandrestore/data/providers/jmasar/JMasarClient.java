package org.phoebus.applications.saveandrestore.data.providers.jmasar;

import org.phoebus.applications.saveandrestore.data.DataProviderException;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;

import java.util.List;

/**
 * Implementations of this interface shall handle the communication to the JMasar service.
 */
public interface JMasarClient {

    String getServiceUrl();

    Node getRoot();

    Node getNode(String uniqueNodeId);

    Node getParentNode(String unqiueNodeId);

    List<Node> getChildNodes(Node node) throws DataProviderException ;

    List<SnapshotItem> getSnapshotItems(String snapshotUniqueId);

    Node saveSnapshot(String configUniqueId, List<SnapshotItem> snapshotItems, String snapshotName, String comment);

    List<ConfigPv> getConfigPvs(String configUniqueId);

    Node createNewNode(String parentsUniqueId, Node node);

    Node updateNode(Node nodeToUpdate);

    Node updateNode(Node nodeToUpdate, boolean customTimeForMigration);

    @Deprecated
    void deleteNode(String uniqueNodeId);

    void deleteNodes(List<String> nodeIds);

    Node updateConfiguration(Node configToUpdate, List<ConfigPv> configPvList);

    List<Tag> getAllTags();

    List<Node> getAllSnapshots();

    Node moveNodes(List<String> sourceNodeIds, String targetNodeId);
}
