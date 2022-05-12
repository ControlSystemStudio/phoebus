Save-and-restore service
========================

The save-and-restore service implements the MASAR (MAchine SAve and Restore) service as a collection
of REST endpoints. These can be used by clients to manage save sets (aka configurations) and
snapshots, to compare snapshots and to restore settings from snapshots.

The service is packaged as a self-contained Spring Boot jar file, i.e. there are no external dependencies besides the
JVM and the database engine persisting the data. The service is verified for Postgresql and Mysql, but alternative
engines can be added with moderate effort, see below for details.

Running the service
-------------------

To run the service, connection parameters for the database must be specified on the command line, or
in existing property files (mysql.properties or postgresql.properties). Typical command line would be:

``java -Ddbengine=[postgresql|mysql]
-Dspring.datasource.username=<DB user name>
-Dspring.datasource.password=<DB password>
-Dspring.datasource.jdbcUrl=<DB engine URL>
-jar /path/to/service-save-and-restore-<version>.jar``

Where

``-Ddbengine`` must be specified to either of the supported database engines. This parameter selects the properties
file containing other settings (mysql.properties or postgresql.propties).

``-Dspring.datasource.username`` specifies the database engine user name. Can be specified in the properties file.

``-Dspring.datasource.password`` specifies the database engine password. Can be specified in the properties file.

``-Dspring.datasource.jdbcUrl`` specifies the database URL required by the JDBC driver. Can be specified in the
properties file.

Database setup
--------------

In order to deploy the service, one must create a database (schema) in the selected database engine matching the
connection paramaters. When the service is started, Flyway scripts will create the required tables. New versions
of the service that require changes to the database structure will also use Flyway scripts to perform necessary
actions on the database.

Alternative database engines
----------------------------

Currently the save-and-restore service does not use an ORM layer (e.g. Hibernate). To support a database engine
other than Postgresql or Mysql, use this checklist:

- Include the required JDBC driver.
- Create a <my favourite DB engine>.properties file containig the driver class name and paths to Flyway scripts.
  The name of the file must match the dbengine value on the command line.
- Create Flyway scripts for the database. Use existing as starting point.
- Configure command line paramaters.
- Verify.

Note that the persistence layer contains hard coded SQL which may be invalid for other database engines. If
there is a need to modify the SQL statement, please discuss this with the community as addition of ORM may be a
better alternative.

REST API for Save Restore Service
=================================

**Configuration:**

A Save Restore configuration is a set of PV's which are used to take a snapshot.
The configuration can also consist of a few options parameters.

- readback pv associated with the pv
- flag to indicate if this pv should restored

The configurations can be organized in the file system like directory structure.


**Snapshot:**

A Save set snapshot consists of a list ov pvs along with their values at a particular instant in time.

REST Services
-------------

The service is implemented as a REST style web service, which – in this context – means:

| •  The URL specifies the data element that the operation works upon.
| •  The HTTP method specifies the type of operation.

| GET: retrieve or query, does not modify data
| PUT: create or update, replacing the addressed element
| POST: create or update subordinates of the addressed element
| DELETE: delete the addressed element


Configuration Management
------------------------

Get the root node
"""""""""""""""""

**.../root**

Method: GET

Return:
The root node of the save restore configuration tree

.. code-block:: JSON

    {
        "id": 0,
        "uniqueId": "25132263-9bee-41ef-8647-fb91632ab9a8",
        "name": "Root folder",
        "created": 1623700954000,
        "lastModified": 1623701038000,
        "nodeType": "FOLDER",
        "userName": "Developer",
        "properties": {
            "root": "true"
        },
        "tags": []
    }


Get a node
""""""""""

**.../node/{uniqueNodeId}**

Method: GET

Return:
The details of the node with id `{uniqueNodeId}`

.. code-block:: JSON

    {
        "id": 3,
        "uniqueId": "ae9c3d41-5aa0-423d-a24e-fc68712b0894",
        "name": "CSX",
        "created": 1623701056000,
        "lastModified": 1623780701000,
        "nodeType": "FOLDER",
        "userName": "kunal",
        "properties": {},
        "tags": []
    }

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

Create a new node
"""""""""""""""""

**.../node/{parentsUniqueId}**

Method: PUT

Body:

.. code-block:: JSON

    {
        "name": "New_Node_Camera",
        "nodeType": "CONFIGURATION",
        "userName": "kunal",
        "properties": {},
        "tags": []
    }

nodeType: "CONFIGURATION" or "FOLDER"

The nodeType can be used to specify if we want to create a new folder or a new save set configuration

Return:
If the node was successfully created you will a 200 response with the details of the newly created node

.. code-block:: JSON

    {
        "id": 21,
        "uniqueId": "c4302cfe-60e2-46ec-bf2b-dcd13c0ef4c0",
        "name": "New_Node_Camera",
        "created": 1625837873000,
        "lastModified": 1625837873000,
        "nodeType": "CONFIGURATION",
        "userName": "kunal",
        "properties": {},
        "tags": []
    }

Create or Update a configuration
""""""""""""""""""""""""""""""""

**.../config/{uniqueNodeId}/update**

Method: POST

Body:

.. code-block:: JSON

    {
        "config": {
            "uniqueId": "8cab9311-0c77-4307-a508-a33677ecc631",
            "userName": "kunal"
        },
        "configPvList" :
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


Snapshot Management
--------------------

Retrieve all snapshots
""""""""""""""""""""""

**.../snapshots**

Method: GET

Retrieve all Snapshots id's

Return:
A list of all the snapshot id's

.. code-block:: JSON

    [
        {
        "id": 21,
        "uniqueId": "c4302cfe-60e2-46ec-bf2b-dcd13c0ef4c0",
        "name": "New_Node_Camera",
        "created": 1625837873000,
        "nodeType": "SNAPSHOT",
        ...
        },
        {
        "id": 22,
        "uniqueId": "c4302cfe-60e2-46ec-bf2b-dad64db1f06d",
        "name": "New_Node_Camera",
        "created": 1625837874000,
        "nodeType": "SNAPSHOT",
        ...
        }
    ]


Retrieve all snapshots for a configuration
""""""""""""""""""""""""""""""""""""""""""

**.../snapshot/{uniqueNodeId}

Retrieve a Snapshot without all the data identified by the `{uniqueNodeId}`


Return:
A snapshot with all the metadata

.. code-block:: JSON
    [
        {
        "id": 21,
        "uniqueId": "c4302cfe-60e2-46ec-bf2b-dcd13c0ef4c0",
        "name": "New_Node_Camera",
        "created": 1625837873000,
        "nodeType": "SNAPSHOT",
        ...
        }
    ]

Retrieve snapshots data
"""""""""""""""""""""""

**.../snapshot/{uniqueNodeId}/items

Method: GET

Retrieve all Snapshots associated with a particular configuration identified by `{uniqueNodeId}`

Return:
A snapshot with all the stored data


.. code-block:: JSON

    [
      {
        "snapshotId": "4099",
        "configPv": {
          "id": 33,
          "pvName": "ISrc-010:Vac-VVMC-01100:FlwSPS",
          "readbackPvName": null,
          "readOnly": false
        },
        "value": {
          "type": {
            "name": "VDouble",
            "version": 1
          },
          "value": 3.5,
          "alarm": {
            "severity": "NONE",
            "status": "NONE",
            "name": "NONE"
          },
          "time": {
            "unixSec": 1635087714,
            "nanoSec": 327966491
          },
          "display": {
            "units": ""
          }
        },
        "readbackValue": null
      },
      {
        "snapshotId": 4099,
        "configPv": {
          "id": 4076,
          "pvName": "LEBT-CS:PwrC-PSRep-01:Vol-S",
          "readbackPvName": null,
          "readOnly": false
        },
        "value": {
          "type": {
            "name": "VDouble",
            "version": 1
          },
          "value": 3.5,
          "alarm": {
            "severity": "NONE",
            "status": "NONE",
            "name": "NONE"
          },
          "time": {
            "unixSec": 1634899034,
            "nanoSec": 639928152
          },
          "display": {
            "units": ""
          }
        },
        "readbackValue": null
      }
    ]