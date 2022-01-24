Save-And-Restore
================

Overview
--------

The save-and-restore application can be used to take a snapshot of a pre-defined list if PVs at a certain point in
time, and write the persisted values back to the IOCs at some later point.

The application depends on the save-and-restore service deployed on the network such that it can be accessed over
HTTP. The URL of the service is specified in the save-and-restore.properties file or in the settings file
pointed to on the command line.

Connection to PVs works the same as for OPI control widgets. The preference org.phoebus.pv/default will determine
how the connection will be set up (ca or pva), but user may explicitly select protocol by using scheme prefix
ca:// or pva://, e.g. ca://my_PV.

Nodes and node types
--------------------

Save-and-restore data managed by the service is arranged in a tree-like structure and hence presented using
a tree view UI component. In the following objects in the tree are referred to as "nodes". The root of the tree
structure is a folder that may only contain folder nodes. Folders may contain sub-folders or save sets, or both.
The child nodes of a save set are snapshots associated with that save set.

There are three node types managed in the application:

- **Folder**: container of other folders or save sets.
- **Save set**: a list of PV names and associated meta-data.
- **Snapshot**: the PV values read from PVs listed in a save set.

*NOTE*: If a folder or save set node is deleted, all child nodes are unconditionally and recursively deleted! The user
is prompted to confirm delete actions as deletion is irreversible.

Below screen shot shows the tree structure and a save set editor.

.. image:: images/screenshot1.png
   :width: 80%

A word of caution
-----------------

Nodes maintained in save-and-restore are persisted in a central service and consequently accessible by multiple
clients. Users should keep in mind that changes (e.g. new or deleted nodes) are not pushed to all connected clients.
Caution is therefore advocated when working on the nodes in the tree, in particular when changing the structure by
deleting or moving nodes.

Drag-n-drop
-----------

Nodes in the tree can be copied (mouse + modifier key) or moved using drag-n-drop. The following restrictions apply:
* Only folder and save set nodes can be copied or moved.
* Save set nodes cannot be copied or moved to the root folder node.
* Target node (aka drop target) must be a folder.

Checks are performed on the service to enforce the above restrictions. If pre-conditions are not met when the selection
is dropped, the application will present an error dialog.

Drag-n-drop is disabled if multiple nodes are selected and if:
* Selection contains a combination of folder and save set nodes. All selected nodes must be of same type.
* Selection contains nodes with different parent nodes. All selected nodes must have the same parent node.

Once a selection of nodes have been copied or moved successfully, the target folder is refreshed to reflect the change.

**NOTE**: Copying a large number of nodes and/or nodes with deep sub-trees is discouraged as this is an expensive operation.
Moving nodes on the other hand is lightweight as only references in the tree structure are updated.