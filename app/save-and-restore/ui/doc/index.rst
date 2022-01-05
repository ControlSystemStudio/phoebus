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

Object types
------------

There are three object types managed by the save-and-restore service through the application:

- Folder: container of other folders or save sets.
- Save set: a list of PV names and associated meta-data.
- Snapshot: the PV values read from PVs listed in a save set.

All objects are managed as nodes of a tree structure. The root of the tree structure is a folder that may only
contain folder objects. Folders may contain sub-folders or save sets, or both. The child nodes of a save set are
snapshots associated with that save set.

*NOTE*: If a folder or save set node is deleted, all child nodes are unconditionally and recursively deleted! The user
is prompted to confirm delete actions.

Below screen shot shows the tree structure and a save set editor.

.. image:: images/screenshot1.png
   :width: 80%

A word of caution
-----------------

Objects (nodes) maintained in save-and-restore are persisted in a central service and consequently accessible by
connected clients. Users should consider that changes (e.g. new or deleted nodes) are not pushed to clients.
Caution is therefore advocated when working on the nodes in the tree, in particular when changing the structure by
deleting or moving nodes.

Drag-n-drop
-----------

Nodes in the tree can be moved using drag-n-drop mouse gestures, with the following restrictions:

* Only folder and save set nodes can be moved.
* Target node (aka drop target) must be a folder node.
* If multiple nodes are selected they must all be of same type (folder or save set), and they must all have the same parent node.

Checks are performed on the service to enforce the above restrictions. If pre-conditions are not met when the selection
is dropped, the application will present an error dialog.

Once a selection of nodes have been moved successfully, the target node is refreshed to reflect the change.