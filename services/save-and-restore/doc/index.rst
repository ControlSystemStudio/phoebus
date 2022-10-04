Save-and-restore service
========================

The save-and-restore service implements service as a collection
of REST endpoints. These can be used by clients to manage configurations(aka save sets) and
snapshots, to compare snapshots and to restore settings from snapshots.

The service is packaged as a self-contained Spring Boot jar file, i.e. there are no external dependencies besides the
JVM and the Elasticsearch instance persisting the data. The service is verified for Elasticsearch 8.x.

Running the service
-------------------

The file ``application.properties`` lists a few settings that can be customized to each site's need, e.g.
connection parameters for Elasticsearch.

Elasticsearch setup
-------------------

During startup the service will create Elasticsearch indices if they are not found. There is hence no need to
do this manually.

REST API for Save Restore Service
=================================

Terminology
-----------

**Configuration:**

A Save Restore configuration is a set of PV's which are used to take a snapshot.
The configuration can also consist of a few optional parameters.

- readback PV associated with the pv
- flag to indicate if the PV should restored in a restore operation

**Snapshot:**

A snapshot consists of a list of PVs along with their values at a particular instant in time. When taking a snapshot
in a client, the list of PVs in the snapshot is defined by the list of PVs in the parent configuration node. In
other words, to take a snapshot the client must point to a configuration defining this list of PVs.

REST Services
-------------

The service is implemented as a REST style web service, which – in this context – means:

| •  The URL specifies the data element that the operation works upon.
| •  The HTTP method specifies the type of operation.

| GET: retrieve or query, does not modify data
| PUT: create or update, replacing the addressed element
| POST: create or update subordinates of the addressed element
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
            "id": 4,
            "uniqueId": "8cab9311-0c77-4307-a508-a33677ecc631",
            "name": "Camera",
            "created": 1623701073000,
            "lastModified": 1625836981000,
            "nodeType": "CONFIGURATION",
            "userName": "kunal",
            "properties": {},
            "tags": []
        },
        {
            "id": 13,
            "uniqueId": "3aa5baa3-8386-4a74-84bb-5fdd9afccc7f",
            "name": "ROI",
            "created": 1623780701000,
            "lastModified": 1623780701000,
            "nodeType": "CONFIGURATION",
            "userName": "kunal",
            "properties": {},
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
configuration data must be listed in the body. Any PVs in the existing configuration data that are missing from the
body will be removed..


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


