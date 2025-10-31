Scan Data Log
=============

The `Log` command to log samples and the associated `scan/{id}/data` REST interface to fetch logged data
are based on an Apache Derby database.
It is stored in the `<data_log>` folder provided in the `scan_config.xml`,
with the example value of `/tmp/scan_log_db`.

It’s a relational database. To access the RDB, the Derby “ij” command line tool is included in the scan server.
In addition to the Derby jar files, the scan server jar is required because the scan sample "value"
is stored as a serialized `org.csstudio.scan.server.log.derby.SampleValue`.

Example for starting "ij" and accessing the basic tables:

```
$ cd scan-server/lib
$ java -Dderby.system.home=/tmp/scan_log_db  \
       -cp "service-scan-server-4.7.4-SNAPSHOT.jar:lib/derby-10.16.1.1.jar:lib/derbyshared-10.16.1.1.jar:lib/derbytools-10.16.1.1.jar” \
        org.apache.derby.impl.tools.ij.Main
ij version 10.16
ij> CONNECT 'jdbc:derby:scan';
ij> SELECT * FROM scans;
ID |NAME    |CREATED                      
---------------------------------------
1  |Example |2025-10-31 11:22:55.760598

ij> SELECT * FROM devices;
ID  |NAME                                                                                                
---------------
1   |loc://x(0)                                                                                          

ij> SELECT * FROM scans;
ID  |NAME         |CREATED                      
---------------------------------------------
1   |Example      |2025-10-31 11:22:55.760598   

ij> SELECT * from samples;
SCAN_ID|DEVICE_ID |SERIAL  |TIMESTAMP                  |VALUE          
-------------------------------------------------------------
1      |1         |0       |2025-10-31 11:22:55.82952  |1.0            
```

Joining the tables together as is tradition with RDBs:

```
ij> SELECT c.name, s.serial, s.timestamp, d.name, s.value
    FROM samples s
    JOIN scans c on c.ID = s.SCAN_ID
    JOIN devices d ON d.ID = s.DEVICE_ID
    ORDER BY c.name, s.serial;

NAME     |SERIAL    |TIMESTAMP                    |NAME          |VALUE          
-----------------------------------------------------------------------
Example  |0         |2025-10-31 11:22:55.82952    |loc://x(0)    |1.0            
```
