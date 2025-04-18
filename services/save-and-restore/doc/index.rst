Save-and-restore service
========================

The save-and-restore service implements service as a collection
of REST endpoints. These can be used by clients to manage configurations (aka save sets) and
snapshots, to compare snapshots and to restore PV values from snapshots.

The service is packaged as a self-contained Spring Boot jar file. External dependencies are limited to a JVM (Java 17+)
and a running instance of Elasticsearch (8.x).

Running the service
-------------------

The file ``application.properties`` lists a few settings that can be customized to each site's need, e.g.
connection parameters for Elasticsearch.

Server-side IOC communication
-----------------------------

The service exposes endpoints for reading and writing PVs, i.e. to create or restore snapshots. Depending on the
setup this server-side IOC communication may need some configuration:

For ca (channel access) the service must be started with the ``-Djca.use_env=true`` Java option, and the list of
gateways - if any - must be set as a system environment named ``EPICS_CA_ADDR_LIST``.

For pva (pv access) the service must be started with the ``-DdefaultProtocol=pva`` Java option, and the list of
gateways - if any - must be set as a system environment named ``EPICS_PVA_ADDR_LIST``.

Elasticsearch setup
-------------------

There is no need to manually created the Elasticsearch indices as these are created by the application if
they do not yet exist.

REST API for Save Restore Service
=================================

Swagger UI
----------

A Swagger UI is by default available on ``.../swagger-ui/index.html``. This should be considered the reference
documentation of the API endpoints.

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

Retrieve multiple nodes
"""""""""""""""""""""""
Method: GET

Body:

.. code-block:: JSON

   ["nodeId-1", "nodeId-2",..., "nodeId-N"]

Return:
Details of the nodes listed as unique node ids in the request body.


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

Delete nodes
""""""""""""

**.../node

Method: DELETE

Caller must specify a body, which is a list of the unique ids of the nodes subject to deletion:

.. code-block:: JSON

    ["id_1", "id_2",...., "id_n"]

Note that deletion is recursive and non-reversible:

- Deleting a configuration node will also delete all associated snapshot nodes.
- Deleting a folder node will delete also delete all nodes in its sub-tree.

Get a node's parent
"""""""""""""""""""

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

.. _Get a configuration:

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
                    "pvName": "13SIM1:{SimDetector-Cam:1}cam1:BinY",
                    "comparison":{
                        "comparisonMode":"ABSOLUTE",
                        "tolerance":2.7
                    }
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

The ``comparison`` field is optional and can be set individually on each element in the list.

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
                        "readOnly": false,
                        "comparison": {
                            "comparisonMode":"ABSOLUTE",
                            "tolerance": 2.7
                        }
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

The ``comparison`` field is optional and can be set individually on each element in the list. If specified,
the ``comparisonMode`` must be either "ABSOLUTE" or "RELATIVE", and the ``tolerance`` must be >=0.

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

.. _Get a snapshot:

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

**.../composite-snapshot/{uniqueId}/items**

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

Server Take Snapshot Endpoints
------------------------------

**.../take-snapshot/{configNodeId}**

Method: GET

This will read PV values for all items listed in the configuration identified by ``configNodeId``. Upon successful
completion, the response will hold an array of objects where each element is on the form:

.. code-block:: JSON

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

**.../take-snapshot/{configNodeId}<?name=snapshotName&comment=snapshotComment>**

Method: PUT

This will read PV values for all items listed in the configuration identified by ``configNodeId``. Upon successful
completion the data is persisted into the database. ``name`` and ``comment`` are optional query parameters and will
default to the current date/time on the format ``yyyy-MM-dd HH:mm:ss.SSS``.

The response is a snapshot object representing the persisted data, see :ref:`Get a snapshot`

Server Restore Endpoints
------------------------

Restore from snapshot items
"""""""""""""""""""""""""""

**.../restore/items**

Method: POST

This endpoint allows you to send a list of ``SnapshotItem`` and the save-and-restore server
will set the values of the PVs in your system to the values supplied.
This allows restoring from clients which do not support EPICS access, for example web clients.

Body:

.. code-block:: JSON

    [
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
        }
    ]

Return: A list of objects associated with PVs that could not be written for whatever reason. If
the list is empty, all PVs in the snapshot were restored correctly.

.. code-block:: JSON

    [
        {
        "snapshotItem": {
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
        "errorMsg": "Connection to PV 'COUNTER10' timed out after 5000ms."
        }
    ]

Restore from snapshot node
""""""""""""""""""""""""""

**.../restore/node?nodeId=<snapshot node id>**

Method: POST

This is the same as the endpoint to restore from snapshot items, however it uses snapshot items
from an existing node rather than providing them explicitly. It returns the same result.

Compare Endpoint
----------------

**.../compare/{uniqueId}[?tolerance=<tolerance_value>&compareMode=<ABSOLUTE|RELATIVE>&skipReadback=<true|false>]**

Method: GET

The path variable ``{uniqueId}`` must identify an existing snapshot or composite snapshot.

The ``tolerance`` query parameter is optional and defaults to zero. If specified it must be >= 0. Non-numeric values
will trigger a HTTP 400 response.

The ``compareMode`` query parameter is optional and defaults to ``ABSOLUTE``. This is case sensitive, values other
than ``ABSOLUTE`` or ``RELATIVE`` will trigger a HTTP 400 response.

The ``skipReadback`` query parameter is optional and defaults to ``false``. This is case insensitive, values
that cannot be evaluated as boolean will trigger a HTTP 400 response.

This endpoint can be used to compare stored snapshot values to live values for each set-point PV in the snapshot. The
reference value for the comparison is always the one corresponding to the ``PV Name`` column in the configuration.

Comparisons are performed like so:

* Scalar PVs are compared using the tolerance and compare mode (absolute or relative), or compared using zero tolerance.
* Array PVs are compared element wise, always using zero tolerance. Arrays must be of equal length.
* Table PVs are compared element wise, always using zero tolerance. Tables must be of same dimensions, and data types must match between columns.
* Enum PVs are compared using zero tolerance.

The ``compareMode`` and ``tolerance`` are applied to all comparison operations, but can be overridden on each individual
item in a configuration, see :ref:`Get a configuration`.

Equality between a stored value and the live value is determined on each PV like so:

* If the configuration of a PV does not specify a comparison mode and tolerance, the ``comparisonMode`` and ``tolerance`` request parameters are used. These however are optional and default to ABSOLUTE and zero respectively.
* The base (reference) value is always the value stored in the value field of a snapshot item object. It corresponds to the ``pvName``` field, i.e. never the ``readbackPvName`` of a configuration item.
* The live value used in the comparison is either the value corresponding to ``pvName``, or ``readbackPvName`` if specified. The latter can be overridden with the ``skipReadback`` request parameter.

Return value: a list of comparison results, one for each PV in the snapshot, e.g.:

.. code-block:: JSON

    {
        "equal" : "false",
        "pvCompareMode": "RELATIVE",
        "tolerance" : 0,
        "storedValue": {
          "type": {
            "name": "VInt",
            "version": 1
          },
          "value": 18,
          "alarm": {
            "severity": "NONE",
            "status": "NONE",
            "name": "NONE"
          },
          "time": {
            "unixSec": 1653903750,
            "nanoSec": 532912758
          },
          "display": {
            "units": ""
          }
        },
        "liveValue": {
          "type": {
            "name": "VInt",
            "version": 1
          },
          "value": 14,
          "alarm": {
            "severity": "NONE",
            "status": "NONE",
            "name": "NO_ALARM"
          },
          "time": {
            "unixSec": 1734688284,
            "nanoSec": 605970324,
            "userTag": 0
          },
          "display": {
            "lowDisplay": 0.0,
            "highDisplay": 255.0,
            "units": "",
            "description": "Mapping for Pulser 0"
          }
        },
        "delta": "+4"
    }

Note that if the comparison evaluates to "equal", then ``storedValue`` and ``liveValue`` are set to ``null``.
The ``delta`` field value is formatted in the same manner as the delta column in the client UI.

Authentication and Authorization
================================

All non-GET endpoints are subject to authentication, i.e. clients must send a basic authentication header. The
service can be configured to delegate authentication to Active Directory or remote or local LDAP. For demo and test
purposes hard coded credentials are found in the ``WebSecurityConfig`` class. See the file ``application.properties``
for information on how to select authentication method.

Two roles are defined, "sar-user" and "sar-admin". The actual name of these roles can be customizable in ``application.properties``,
and must match role/group names in LDAP or Active Directory.

Authorization uses a role-based approach like so:

* Unauthenticated users may read data, i.e. access GET endpoints.
* Save-and-restore role "sar-user":
    * Create and update configurations
    * Create and update snapshots
    * Create and update composite snapshots
    * Create and update filters
    * Create and update tags, except GOLDEN tag
    * Update and delete objects if user name matches object's user id and:
        * Object is a snapshot node and not referenced in a composite snapshot node
        * Object is a composite snapshot node
        * Object is configuration or folder node with no child nodes
        * Object is a filter
        * Object is a tag
* Save-and-restore role "sar-admin": no restrictions

Authentication endpoint
"""""""""""""""""""""""

A client can may use the /login endpoint to check if user is authenticated:

**.../login**

Method: POST

Body:

.. code-block:: JSON

    {"username":"johndoe", "password":"undisclosed"}


Enabled authentication, disabled authorization
----------------------------------------------

The application property ``authorization.permitall`` (default ``true``) can be used to bypass all authorization. In
this case authentication is still required for protected endpoints, but user need not be associated with
a save-and-restore role/group.

Migration
=========

From commit ``48e17a380b660d59b79cec4d2bd908c0d78eeeae`` of the service code base the persistence
layer is moved from RDB engine to Elasticsearch. Sites using save-and-restore with an RDB engine may migrate
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


