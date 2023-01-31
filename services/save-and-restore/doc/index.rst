Save-and-restore service
========================

The save-and-restore service implements service as a collection
of REST endpoints. These can be used by clients to manage configurations (aka save sets) and
snapshots, to compare snapshots and to restore PV values from snapshots.

The service is packaged as a self-contained Spring Boot jar file. External dependencies are limited to a JVM (Java 11+)
and a running instance of Elasticsearch (8.x).

Running the service
-------------------

The file ``application.properties`` lists a few settings that can be customized to each site's need, e.g.
connection parameters for Elasticsearch.

Elasticsearch setup
-------------------

There is no need to manually created the Elasticsearch indices as these are created by the application if
they do not yet exist.

REST API for Save Restore Service
=================================

Node
----

Data is arranged such that is can be rendered in a tree structure, where each node is of a specific type. See below
for details. A root node is always available and cannot be deleted.

Each node is uniquely identified through an UUID id. The root node's unique id is always
``44bef5de-e8e6-4014-af37-b8f6c8a939a2``.

REST end-points documented below can be used to locate particular nodes, or traverse the tree by listing child
nodes.

Node types
----------

**Folder:**

A folder node is a container for folder and configuration nodes. The root node is a folder node.

**Configuration:**

A configuration node is essentially a set of PVs defining what data to put in a snapshot. Configuration nodes must be created
in folder nodes, though not in the root node.

For each such PV "item" one may also specify:

- a read-back PV
- flag to indicate if the PV should restored in a restore operation

**Snapshot:**

A snapshot node consists of a list of PV values at a particular instant in time. To take a snapshot the client must point to
a configuration defining this list of PVs (and optionally read-back PVs). In other words, when saving a snapshot
the client must specify the unique id of the associated configuration node.

**Composite Snapshot:**

An aggregation of snapshot nodes and/or other composite snapshot nodes. The referenced nodes must exist in order
to be able to create a composite snapshot. Moreover, a snapshot node cannot be deleted if it is referenced in
a composite snapshot.

REST Services
-------------

The service is implemented as a REST style web service, which – in this context – means:

| •  The URL specifies the data element that the operation works upon.
| •  The HTTP method specifies the type of operation.

| GET: retrieve an element, does not modify data
| PUT: create an element
| POST: update the addressed element
| DELETE: delete the addressed element


Node Management
---------------

Get a node
""""""""""

**.../node/{uniqueNodeId}**

Method: GET

Return:
The details of the node with id `{uniqueNodeId}`

.. code-block:: JSON

    {
        "uniqueId": "ae9c3d41-5aa0-423d-a24e-fc68712b0894",
        "name": "CSX",
        "created": 1623701056000,
        "lastModified": 1623780701000,
        "nodeType": "FOLDER",
        "userName": "kunal",
        "tags": []
    }

Nodes of type CONFIGURATION and SNAPSHOT will also have a ``description`` field.

A special case is the root node as it has a fixed unique id:

**.../node/44bef5de-e8e6-4014-af37-b8f6c8a939a2**

Create a new node
"""""""""""""""""

**.../node?parentNodeId=<parent's node id>**

Method: PUT

Body:

.. code-block:: JSON

    {
        "name": "New_Node_Camera",
        "nodeType": "CONFIGURATION",
        "userName": "kunal"
    }

nodeType: "CONFIGURATION" or "FOLDER". The request parameter ``parentNodeId`` is mandatory and must identify an
existing folder node.

The nodeType can be used to specify if we want to create a new folder or a new configuration.

Return:
If the node was successfully created you will a 200 response with the details of the newly created node

.. code-block:: JSON

    {
        "uniqueId": "c4302cfe-60e2-46ec-bf2b-dcd13c0ef4c0",
        "name": "New_Node_Camera",
        "created": 1625837873000,
        "lastModified": 1625837873000,
        "nodeType": "CONFIGURATION",
        "userName": "kunal",
        "tags": []
    }

Update a node
"""""""""""""

**.../node**

Method: POST

Return:
The updated node.

.. code-block:: JSON

    {
        "uniqueId": "ae9c3d41-5aa0-423d-a24e-fc68712b0894",
        "name": "new name",
        "description": "new description",
        "created": 1623701056000,
        "lastModified": 1623780701000,
        "nodeType": "CONFIGURATION",
        "userName": "kunal",
        "tags": []
    }

Updates an existing node with respect to its name or description, or both. The ``nodeType`` cannot be
updated.

Delete a node
"""""""""""""

**.../node/{uniqueNodeId}**

Method: DELETE

Deletes the node identified by ``uniqueNodeId``. Deletion is agnostic to the node type.

Note that deletion is recursive:

- Deleting a configuration node will also delete all associated snapshot nodes.
- Deleting a folder node will delete also delete all nodes in its sub-tree.

Get a node parent
"""""""""""""""""

**.../node/{uniqueNodeId}/parent**

Method: GET

Return:
The details of the *parent* node of the node with id `{uniqueNodeId}`

Get children
""""""""""""

**.../node/{uniqueNodeId}/children**

Method: GET

Return:
The a list of all the children nodes of the node with id `{uniqueNodeId}`

.. code-block:: JSON

    [
        {
            "uniqueId": "8cab9311-0c77-4307-a508-a33677ecc631",
            "name": "Camera",
            "created": 1623701073000,
            "lastModified": 1625836981000,
            "nodeType": "CONFIGURATION",
            "userName": "kunal",
            "tags": []
        },
        {
            "uniqueId": "3aa5baa3-8386-4a74-84bb-5fdd9afccc7f",
            "name": "ROI",
            "created": 1623780701000,
            "lastModified": 1623780701000,
            "nodeType": "CONFIGURATION",
            "userName": "kunal",
            "tags": []
        }
    ]

Get a configuration
"""""""""""""""""""

To get a configuration node the client should call the end-point associated with getting nodes of any type:

**.../node/{uniqueNodeId}**

where ``uniqueNodeId`` identifies the configuration node.

The actual configuration data associated with a configuration node is maintained in a separate Elasticsearch index and
is accessible through:

**.../config/{uniqueNodeId}**

where ``uniqueNodeId`` identifies the configuration node.

Method: GET

Return: object describing the configuration data, essentially a list of PVs.

.. code-block:: JSON

    {
        "uniqueId": "89886b32-bb2e-4336-8eea-375c0a955cad",
        "pvList": {
            [
                {
                    "pvName": "13SIM1:{SimDetector-Cam:1}cam1:BinX"
                },
                {
                    "pvName": "13SIM1:{SimDetector-Cam:1}cam1:BinY"
                },
                {
                    "pvName": "13SIM1:{SimDetector-Cam:2}cam2:BinX",
                    "readbackPvName": null,
                    "readOnly": false
                },
                {
                    "pvName": "13SIM1:{SimDetector-Cam:2}cam2:BinY",
                    "readbackPvName": null,
                    "readOnly": false
                }
            ]
        }
    }

Here the ``uniqueId`` field matches the ``unqiueId`` field of the configuration node.

Create a configuration
""""""""""""""""""""""

**.../config?parentNodeId=<parent's node id>**

Method: PUT

Return: an object representing the saved configuration. This object is of the same type as
the body sent in the request, with additional data set by the service, e.g. the unique id of the
created configuration node.

Body:

.. code-block:: JSON

    {
        "configurationNode": {
             "name": "New_Configuration",
             "nodeType": "CONFIGURATION",
             "userName": "kunal"
        },
        "configurationData": {
            "pvList": {
                [
                    {
                        "pvName": "13SIM1:{SimDetector-Cam:1}cam1:BinX"
                    },
                    {
                        "pvName": "13SIM1:{SimDetector-Cam:1}cam1:BinY"
                    },
                    {
                        "pvName": "13SIM1:{SimDetector-Cam:2}cam2:BinX",
                        "readbackPvName": null,
                        "readOnly": false
                    },
                    {
                        "pvName": "13SIM1:{SimDetector-Cam:2}cam2:BinY",
                        "readbackPvName": null,
                        "readOnly": false
                    }
                ]
            }
        }
    }

The request parameter ``parentNodeId`` is mandatory and must identify an existing folder node. The client
needs to specify a name for the new configuration node, as well as a user identity.

Update a configuration
""""""""""""""""""""""

**.../config/{uniqueNodeId}**

Method: POST

This endpoint works in the same manner as the for the PUT method, i.e. the body and return value are the
same. However, in this case the ``uniqueNodeId`` must identify an existing configuration node.

The body can specify a new name or description, or both. On top of that the list of PVs can be updated. It should
be noted though that the specified list will replace the existing one, i.e. all PVs that must remain in the updated
configuration data must be listed in the body. Any PVs in the existing configuration data missing from the
body will be removed.


Snapshot Endpoints
------------------

Get a snapshot
""""""""""""""

To get a snapshot node the client should call the end-point associated with getting nodes of any type:

**.../node/{uniqueNodeId}**

where ``uniqueNodeId`` identifies the snapshot node.

The actual snapshot data associated with a snapshot node is maintained in a separate Elasticsearch index and
is accessible through:

**.../snapshot/{uniqueNodeId}**

where ``uniqueNodeId`` identifies the snapshot node.

Method: GET

Return: object describing the snapshot data, essentially a list of PVs and the persisted values.

.. code-block:: JSON

    {
        "uniqueId":"54920ffe-8932-46e6-b420-5b7b20d2cea1",
        "snapshotItems":[
            {
                "configPv": {
                    "pvName":"COUNTER10",
                    "readOnly":false
                },
                "value":{
                    "type":{
                        "name":"VDouble",
                        "version":1
                    },
                    "value":11941.0,
                    "alarm":{
                        "severity":"NONE",
                        "status":"NONE",
                        "name":"NO_ALARM"
                    },
                    "time":{
                        "unixSec":1664550284,
                        "nanoSec":870687555
                    },
                    "display":{
                        "lowDisplay":0.0,
                        "highDisplay":0.0,
                        "units":""
                    }
                }
            },
            {
                "configPv":{
                    "pvName":"TEMP10",
                    "readOnly":false
                },
                "value":{
                    "type":{
                        "name":"VDouble",
                        "version":1
                    },
                    "value":-4.205873713538651,
                    "alarm":{
                        "severity":"MINOR",
                        "status":"NONE",
                        "name":"LOW_ALARM"
                    },
                    "time":{
                        "unixSec":1664550284,
                        "nanoSec":870768480
                    },
                    "display":{
                        "lowAlarm":-5.0,
                        "highAlarm":30.0,
                        "lowDisplay":-60.0,
                        "highDisplay":60.0,
                        "lowWarning":0.0,
                        "highWarning":10.0,
                        "units":"°"
                    }
                }
            }
        ]
    }

To be noted: the ``value`` field is a serialized version of the underlying EPICS PV objects. The contents of
this field will hence depend on the EPICS record type and its properties.

Save a snapshot
"""""""""""""""

**.../snapshot?parentNodeId=<parent's node id>**

Method: PUT

Return: an object representing the saved snapshot. This object is of the same type as
the body sent in the request, with additional data set by the service, e.g. the unique id of the
created snapshot node.

Body:

.. code-block:: JSON

    {
        "snapshotNode": {
             "name": "New_Snapshot",
             "nodeType": "SNAPSHOT",
             "userName": "kunal"
        },
        "snapshotData": {
            "snapshotItems":[
                {
                    "configPv": {
                        "pvName":"COUNTER10",
                        "readOnly":false
                    },
                    "value":{
                        "type":{
                            "name":"VDouble",
                            "version":1
                        },
                        "value":11941.0,
                        "alarm":{
                            "severity":"NONE",
                            "status":"NONE",
                            "name":"NO_ALARM"
                        },
                        "time":{
                            "unixSec":1664550284,
                            "nanoSec":870687555
                        },
                        "display":{
                            "lowDisplay":0.0,
                            "highDisplay":0.0,
                            "units":""
                        }
                    }
                },
                {
                    "configPv":{
                        "pvName":"TEMP10",
                        "readOnly":false
                    },
                    "value":{
                        "type":{
                            "name":"VDouble",
                            "version":1
                        },
                        "value":-4.205873713538651,
                        "alarm":{
                            "severity":"MINOR",
                            "status":"NONE",
                            "name":"LOW_ALARM"
                        },
                        "time":{
                            "unixSec":1664550284,
                            "nanoSec":870768480
                        },
                        "display":{
                            "lowAlarm":-5.0,
                            "highAlarm":30.0,
                            "lowDisplay":-60.0,
                            "highDisplay":60.0,
                            "lowWarning":0.0,
                            "highWarning":10.0,
                            "units":"°"
                        }
                    }
                }
            ]
        }
    }

The request parameter ``parentNodeId`` is mandatory and must identify an existing configuration node. This
configuration node must be the configuration node associated with the snapshot, i.e. must specify the list
of PVs contained in the snapshot. The client needs to specify a name for the new snapshot node, as well as
a user identity.

<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> CSSTUDIO-1707
Composite Snapshot Endpoints
----------------------------

Get a composite snapshot
""""""""""""""""""""""""

To get a composite snapshot node the client should call the end-point associated with getting nodes of any type:

**.../node/{uniqueNodeId}**

where ``uniqueNodeId`` identifies the composite snapshot node.

The actual composite snapshot data associated with a composite snapshot node is maintained in a separate Elasticsearch index and
is accessible through:

**.../composite-snapshot/{uniqueNodeId}**

where ``uniqueNodeId`` identifies the composite snapshot node.

Method: GET

Return: object describing the composite snapshot data, essentially a list of referenced snapshot and composite
snapshot nodes.

.. code-block:: JSON

    {
      "uniqueId": "e80fba66-c7f0-453e-8cb6-12b22fa8c957",
      "referencedSnapshotNodes": [
        "b0cee6ff-76a2-46e6-b0ef-d8b78bff26f6",
        "b6b5a03e-252e-4e6b-a9ac-9d50c23f3f0b"
      ]
    }

Create a composite snapshot
"""""""""""""""""""""""""""

**.../composite-snapshot?parentNodeId=<parent's node id>**

Method: PUT

Return: an object representing the composite snapshot. This object is of the same type as
the body sent in the request, with additional data set by the service, e.g. the unique id of the
created composite snapshot node.

Body:

.. code-block:: JSON
<<<<<<< HEAD
=======
Migration
---------

Commit ``48e17a380b660d59b79cec4d2bd908c0d78eeeae`` of the service code base is about changing the persistence
component from a RDB engine to Elasticsearch. Sites using save-and-restore with an RDB engine may migrate
data using the below procedure.

Terminology: "source host" is the host running the legacy service instance using a RDB engine,
while "target host" is the host that will be running the updated service.

Make sure the source host is running the legacy save-and-restore service.

Make sure the target host is running the Elasticsearch service, but **not** the save-and-restore service.

On the target host, launch the save-and-restore service using the ``-migrate`` program argument:
``java -jar /path/to/service-save-and-restore-<version>.jar -migrate http://<source host>:8080``

Here it is assumed that the legacy save-and-restore service has been published on the (default) port 8080.

If Elasticsearch is not running on localhost:9200, then add Java VM arguments like so:

``-Delasticsearch.network.host=<hostname>``

``-Delasticsearch.http.port=<port>``


>>>>>>> master
=======
>>>>>>> CSSTUDIO-1707

    {
        "compositeSnapshotNode": {
             "name": "New_Composite_Snapshot",
             "nodeType": "COMPOSITE_SNAPSHOT",
             "userName": "johndoe"
        },
        "referencedSnapshotNodes": {
            [
                "b0cee6ff-76a2-46e6-b0ef-d8b78bff26f6",
                "b6b5a03e-252e-4e6b-a9ac-9d50c23f3f0b"
            ]
        }
    }

Update a composite snapshot
"""""""""""""""""""""""""""

**.../composite-snapshot/{uniqueNodeId}**

Method: POST

This endpoint works in the same manner as the for the PUT method, i.e. the body and return value are the
same. However, in this case the ``uniqueNodeId`` must identify an existing composite snapshot node.

The body can specify a new name or description, or both. On top of that the list of referenced snapshots can be updated. It should
be noted though that the specified list will replace the existing one, i.e. all referenced snapshots that must remain in the updated
composite snapshot data must be listed in the body. Any snapshots in the existing configuration data missing from the
body will be removed.

Get restorable items of a composite snapshot
""""""""""""""""""""""""""""""""""""""""""""

***.../composite-snapshot/{uniqueId}/items**

Method: GET

Return: a list of all snapshot items as persisted in the snapshots referenced by a composite snapshot.

Body:

.. code-block:: JSON

    [
      {
        "configPv": {
          "pvName": "RFQ-010:RFS-EVR-101:OpMode",
          "readbackPvName": null,
          "readOnly": false
        },
        "value": {
          "type": {
            "name": "VEnum",
            "version": 1
          },
          "value": 0,
          "alarm": {
            "severity": "NONE",
            "status": "NONE",
            "name": "NONE"
          },
          "time": {
            "unixSec": 1638905851,
            "nanoSec": 445854166
          },
          "enum": {
            "labels": [
              "Global"
            ]
          }
        }
      },
      {
        "configPv": {
          "pvName": "RFQ-010:RFS-EVR-101:RFSyncDly-SP",
          "readbackPvName": null,
          "readOnly": false
        },
        "value": {
          "type": {
            "name": "VDouble",
            "version": 1
          },
          "value": 200.0,
          "alarm": {
            "severity": "NONE",
            "status": "NONE",
            "name": "NONE"
          },
          "time": {
            "unixSec": 1638475923,
            "nanoSec": 703595298
          },
          "display": {
            "units": ""
          }
        }
      },
      {
        "configPv": {
          "pvName": "RFQ-010:RFS-EVR-101:RFSyncWdt-SP",
          "readbackPvName": null,
          "readOnly": false
        },
        "value": {
          "type": {
            "name": "VDouble",
            "version": 1
          },
          "value": 100.0,
          "alarm": {
            "severity": "NONE",
            "status": "NONE",
            "name": "NONE"
          },
          "time": {
            "unixSec": 1639063122,
            "nanoSec": 320431469
          },
          "display": {
            "units": ""
          }
        }
      },
      {
        "configPv": {
          "pvName": "RFQ-010:RFS-EVR-101:SCDly",
          "readbackPvName": null,
          "readOnly": false
        },
        "value": {
          "type": {
            "name": "VDouble",
            "version": 1
          },
          "value": 493.2,
          "alarm": {
            "severity": "NONE",
            "status": "NONE",
            "name": "NONE"
          },
          "time": {
            "unixSec": 1639209326,
            "nanoSec": 372407313
          },
          "display": {
            "units": ""
          }
        }
      }
    ]

Migration
---------

Commit ``48e17a380b660d59b79cec4d2bd908c0d78eeeae`` of the service code base is about changing the persistence
component from a RDB engine to Elasticsearch. Sites using save-and-restore with an RDB engine may migrate
data using the below procedure.

Terminology: "source host" is the host running the legacy service instance using a RDB engine,
while "target host" is the host that will be running the updated service.

Make sure the source host is running the legacy save-and-restore service.

Make sure the target host is running the Elasticsearch service, but **not** the save-and-restore service.

On the target host, launch the save-and-restore service using the ``-migrate`` program argument:
``java -jar /path/to/service-save-and-restore-<version>.jar -migrate http://<source host>:8080``

Here it is assumed that the legacy save-and-restore service has been published on the (default) port 8080.

If Elasticsearch is not running on localhost:9200, then add Java VM arguments like so:

``-Delasticsearch.network.host=<hostname>``

``-Delasticsearch.http.port=<port>``


