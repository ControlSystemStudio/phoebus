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
connected clients. Users should keep in mind that changes (e.g. new or deleted nodes) are not pushed to all clients.
Caution is therefore advocated when working on the nodes in the tree, in particular when changing the structure by
deleting or moving nodes.

Selection of objects in the tree view
-----------------------------------

Multiple selection - using mouse and key combination - of objects in the tree view is supported only if selected objects
are of same type and if they have the same parent. If an unsupported selection is detected, an error dialog is shown and the
selection is cleared.

Drag-n-drop
-----------

Objects in the tree can be copied or moved using drag-n-drop. While move is performed with mouse only,
copy is supported using mouse + modifier key. The following restrictions apply:

* Only folder and save set objects can be moved.
* Target object (aka drop target) must be a folder.

Checks are performed on the service to enforce the above restrictions. If pre-conditions are not met when the selection
is dropped to the target, the application will present an error dialog.

Once a selection of objects have been copied or moved successfully, the target folder is refreshed to reflect the change.

**NOTE**: A copy operation will take some time to execute if the selected objects contain large sub-trees. Users are therefore
encouraged to avoid copy operations of complex sub-trees, or
folders containing a large number of save sets, each with a large number of snapshots. A move
operation on the other hand is lightweight as there is no need to copy data.