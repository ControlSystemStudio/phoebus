
Configuring the Data Browser to read from TimescaleDB
=====================================================

Efficient data retrieval is based on a dedicated archive reader implementation
that calls a server-side stored function.

The stored function is installed with the archive table schema,
see [2 Configure Database](2_ConfigureDatabase.md).
  
The retrieval routine is selected via a "ts:..." URL in the data browser settings,
basically using a `jdbc:postgresql://..` URL prefixed with `ts:`.

    # Offer TimescaleDB in archive search panel
    org.csstudio.trends.databrowser3/urls=ts:jdbc:postgresql://your_host:5432/tsarch|Timescale
    # Use TimescaleDB to read data
    org.csstudio.trends.databrowser3/archives=ts:jdbc:postgresql://your_host:5432/tsarch|Timescale
    
    # Account with read-only TimescaleDB access
    org.csstudio.archive.ts/user=report
    org.csstudio.archive.ts/password=$report
    
