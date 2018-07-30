package org.csstudio.archive.writer.rdb;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

public class PGCopyPreparedStatement implements PreparedStatement {

    private Connection connection;

    private String[] rowValues;

    private StringBuffer batchBuilder;

    private int[] columnOrderMapping;

    private String tableName;

    public PGCopyPreparedStatement(Connection connection, String insertSqlQuery)
            throws SQLException {
        this.connection = connection;
        batchBuilder = new StringBuffer();

        // Analyze query string to get table name and list of column
        tableName = null;
        String[] columnsArrays = null;
        Pattern p = Pattern.compile(
                "^INSERT[ ]+INTO[ ]+([^ ]+)[ ]+\\(([^)]+)\\)",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(insertSqlQuery);
        if (m.find()) {
            tableName = m.group(1);
            columnsArrays = m.group(2).split(",");
        }

        // Get the column order as it's stored in database
        Map<String, Integer> postgresColumnOrderMap = new HashMap<String, Integer>();
        ResultSet columnsRs = connection.getMetaData().getColumns(
                connection.getCatalog(), null, tableName, null);
        while (columnsRs.next()) {
            postgresColumnOrderMap.put(columnsRs.getString("COLUMN_NAME"),
                    columnsRs.getInt("ORDINAL_POSITION"));
        }
        rowValues = new String[postgresColumnOrderMap.size()];

        // Generate a tab containing mapping between order in insert query and
        // database order
        columnOrderMapping = new int[columnsArrays.length + 1];
        columnOrderMapping[0] = -1;
        for (int i = 0; i < columnsArrays.length; i++) {
            String columnName = columnsArrays[i].trim();
            Integer postgresColumnOrder = postgresColumnOrderMap
                    .get(columnName);
            if (postgresColumnOrder == null) {
                throw new SQLException("Unable to find column " + columnName
                        + "  in table " + tableName);
            }
            columnOrderMapping[i + 1] = postgresColumnOrder.intValue() - 1;
        }
    }

    @Override
    public void addBatch() throws SQLException {
        for (int i = 0; i < rowValues.length; i++) {
            if (rowValues[i] != null) {
                batchBuilder.append(rowValues[i]);
            }
            batchBuilder.append(",");
        }
        batchBuilder.setCharAt(batchBuilder.length() - 1, '\n');
        Arrays.fill(rowValues, null);
    }

    @Override
    public void addBatch(String arg0) throws SQLException {
        throw new SQLException("Not implemented");
    }

    @Override
    public void cancel() throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void clearBatch() throws SQLException {
        batchBuilder.setLength(0);
    }

    @Override
    public void clearWarnings() throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void close() throws SQLException {
        rowValues = null;
        columnOrderMapping = null;
        batchBuilder = null;
        connection = null;
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public boolean execute(String arg0) throws SQLException {
        throw new SQLException("Not implemented");
        // return false;
    }

    @Override
    public boolean execute(String arg0, int arg1) throws SQLException {
        throw new SQLException("Not implemented");
        // return false;
    }

    @Override
    public boolean execute(String arg0, int[] arg1) throws SQLException {
        throw new SQLException("Not implemented");
        // return false;
    }

    @Override
    public boolean execute(String arg0, String[] arg1) throws SQLException {
        throw new SQLException("Not implemented");
        // return false;
    }

    @Override
    public int[] executeBatch() throws SQLException {
        long res = 0;
        try {
            CopyManager cpManager = ((PGConnection) connection).getCopyAPI();
            PushbackReader reader = new PushbackReader(new StringReader(""),
                    batchBuilder.length());
            reader.unread(batchBuilder.toString().toCharArray());
            res = cpManager.copyIn("COPY " + tableName +  " FROM STDIN WITH CSV", reader);
            batchBuilder.setLength(0);
            reader.close();
        } catch (IOException e) {
            throw new SQLException(e);
        }
        return new int[] { (int) res };
    }

    @Override
    public ResultSet executeQuery(String arg0) throws SQLException {
        throw new SQLException("Not implemented");
        // return null;
    }

    @Override
    public int executeUpdate(String arg0) throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public int executeUpdate(String arg0, int arg1) throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public int executeUpdate(String arg0, int[] arg1) throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public int executeUpdate(String arg0, String[] arg1) throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connection;
    }

    @Override
    public int getFetchDirection() throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public int getFetchSize() throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLException("Not implemented");
        // return null;
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public int getMaxRows() throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        throw new SQLException("Not implemented");
        // return false;
    }

    @Override
    public boolean getMoreResults(int arg0) throws SQLException {
        throw new SQLException("Not implemented");
        // return false;
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        throw new SQLException("Not implemented");
        // return null;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public int getResultSetType() throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        throw new SQLException("Not implemented");
        // return null;
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        throw new SQLException("Not implemented");
        // return false;
    }

    @Override
    public boolean isClosed() throws SQLException {
        throw new SQLException("Not implemented");
        // return false;
    }

    @Override
    public boolean isPoolable() throws SQLException {
        throw new SQLException("Not implemented");
        // return false;
    }

    @Override
    public void setCursorName(String arg0) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setEscapeProcessing(boolean arg0) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setFetchDirection(int arg0) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setFetchSize(int arg0) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setMaxFieldSize(int arg0) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setMaxRows(int arg0) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setPoolable(boolean arg0) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setQueryTimeout(int arg0) throws SQLException {

    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        throw new SQLException("Not implemented");
        // return false;
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException {
        throw new SQLException("Not implemented");
        // return null;
    }

    @Override
    public void clearParameters() throws SQLException {
        Arrays.fill(rowValues, null);
    }

    @Override
    public boolean execute() throws SQLException {
        throw new SQLException("Not implemented");
        // return false;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        throw new SQLException("Not implemented");
        // return null;
    }

    @Override
    public int executeUpdate() throws SQLException {
        throw new SQLException("Not implemented");
        // return 0;
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        throw new SQLException("Not implemented");
        // return null;
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw new SQLException("Not implemented");
        // return null;
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x)
            throws SQLException {
        if (x == null) {
            rowValues[columnOrderMapping[parameterIndex]] = null;
        } else {
            rowValues[columnOrderMapping[parameterIndex]] = x.toPlainString();
        }
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        rowValues[columnOrderMapping[parameterIndex]] = Boolean.toString(x);
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        if (x == null) {
            rowValues[columnOrderMapping[parameterIndex]] = null;
        } else {
            rowValues[columnOrderMapping[parameterIndex]] = bytesToByteA(x);
        }
    }

    /**
     * <p>
     * Convert to string representation of given byte array to postgreSQL format
     * (octal).
     * </p>
     * <p>
     * See <a
     * href="http://www.postgresql.org/docs/9.2/static/datatype-binary.html"
     * >http://www.postgresql.org/docs/9.2/static/datatype-binary.html</a>
     * </p>
     *
     * @param bytes
     * @return
     */
    public static String bytesToByteA(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 4);
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            sb.append("\\");
            String octal = Integer.toString(v, 8);
            int nbHeadingZeroToadd = 3 - octal.length();
            for (int i = 0; i < nbHeadingZeroToadd; i++) {
                sb.append("0");
            }
            sb.append(octal);
        }
        return sb.toString();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader,
            long length) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        rowValues[columnOrderMapping[parameterIndex]] = Double.toString(x);
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        rowValues[columnOrderMapping[parameterIndex]] = Float.toString(x);
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        rowValues[columnOrderMapping[parameterIndex]] = Integer.toString(x);
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        rowValues[columnOrderMapping[parameterIndex]] = Long.toString(x);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value,
            long length) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setNString(int parameterIndex, String value)
            throws SQLException {
        if (value == null) {
            rowValues[columnOrderMapping[parameterIndex]] = null;
        } else {
            rowValues[columnOrderMapping[parameterIndex]] = value;
        }
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        rowValues[columnOrderMapping[parameterIndex]] = null;
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName)
            throws SQLException {
        rowValues[columnOrderMapping[parameterIndex]] = null;
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType,
            int scaleOrLength) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        rowValues[columnOrderMapping[parameterIndex]] = Short.toString(x);
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        rowValues[columnOrderMapping[parameterIndex]] = x;
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x)
            throws SQLException {
        if (x == null) {
            rowValues[columnOrderMapping[parameterIndex]] = null;
        } else {
            rowValues[columnOrderMapping[parameterIndex]] = x.toString();
        }
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        throw new SQLException("Not implemented");

    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length)
            throws SQLException {
        throw new SQLException("Not implemented");

    }

}
