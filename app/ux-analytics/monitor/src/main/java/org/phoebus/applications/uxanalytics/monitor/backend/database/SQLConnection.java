package org.phoebus.applications.uxanalytics.monitor.backend.database;

import javafx.application.Platform;
import org.phoebus.applications.uxanalytics.monitor.backend.image.FilesystemImageClient;
import org.phoebus.applications.uxanalytics.monitor.backend.image.ImageClient;
import org.phoebus.applications.uxanalytics.monitor.representation.ActiveTab;
import org.phoebus.applications.uxanalytics.monitor.util.FileUtils;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.security.tokens.AuthenticationScope;

import java.net.URI;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLConnection implements BackendConnection{

    Logger logger = Logger.getLogger(SQLConnection.class.getName());
    Connection conn;
    String userHost = null;
    Integer userPort = null;
    String userName = null;
    String password = null;
    String database = null;
    String table = null;
    private ImageClient imageClient;
    private SQLConnection()  {
        tryAutoConnect(AuthenticationScope.MARIADB);
    }
    private static SQLConnection instance;
    public static SQLConnection getInstance(){
        if(instance == null){
            instance = new SQLConnection();
        }
        return instance;
    }

    static class JDBCConnectionBuilder {
        private String protocol = "jdbc:mysql://";
        private String host = null;
        private String port = null;
        private String username = null;
        private String password = null;
        private String database = null;


        public JDBCConnectionBuilder() {
        }

        public JDBCConnectionBuilder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public JDBCConnectionBuilder host(String host) {
            this.host = host;
            return this;
        }

        public JDBCConnectionBuilder port(Integer port) {
            if(port != null)
                this.port = port.toString();
            return this;
        }

        public JDBCConnectionBuilder username(String username) {
            this.username = username;
            return this;
        }

        public JDBCConnectionBuilder password(String password) {
            this.password = password;
            return this;
        }

        public JDBCConnectionBuilder database(String database) {
            this.database = database;
            return this;
        }

        public Connection build() {
            String url = protocol;
            if(host != null)
                url += host;
            else
                url += PhoebusPreferenceService.userNodeForClass(SQLConnection.class).get("host", "localhost");
            if(port != null)
                url += ":" + port;
            else
                url += ":" + PhoebusPreferenceService.userNodeForClass(SQLConnection.class).get("port", "3306");
            if(username == null)
                username = PhoebusPreferenceService.userNodeForClass(SQLConnection.class).get("username", "root");
            if(database != null)
                url += "/" + database;
            else
                url += "/" + PhoebusPreferenceService.userNodeForClass(SQLConnection.class).get("database", "phoebus_analytics");
            try{
                Logger.getLogger(SQLConnection.class.getName()).info("Connecting to JDBC: " + url);
                return DriverManager.getConnection(url, username, password);
            } catch(SQLException e){
                Logger.getLogger(JDBCConnectionBuilder.class.getName()).info("UX Analytics Failed to connect to JDBC: " + e.getMessage());
                return null;
            }
        }
    }

    @Override
    public Boolean connect(String hostOrRegion, Integer port, String usernameOrAccessKey, String passwordOrSecretKey) {
        if(conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to close existing JDBC connection: " + e.getMessage());
            }
        }
        conn = null;
        userHost = hostOrRegion;
        userPort = port;
        userName = usernameOrAccessKey;
        database = PhoebusPreferenceService.userNodeForClass(SQLConnection.class).get("database", "phoebus_analytics");
        conn =  new JDBCConnectionBuilder()
                .host(userHost)
                .port(userPort)
                .username(userName)
                .password(passwordOrSecretKey)
                .database(database)
                .build();
        return (conn != null);
    }

    @Override
    public String getProtocol() {
        return "jdbc:mysql://";
    }

    @Override
    public String getHost() {
        if(userHost != null)
            return userHost;
        return PhoebusPreferenceService.userNodeForClass(SQLConnection.class).get("host", "localhost");
    }

    @Override
    public String getPort() {
        if(userPort != null)
            return userPort.toString();
        return PhoebusPreferenceService.userNodeForClass(SQLConnection.class).get("port", "3306");
    }

    @Override
    public String getDefaultUsername() {
        return "root";
    }

    @Override
    public Integer tearDown() {
        try{
            conn.close();
            return 0;
        } catch(SQLException e){
            logger.warning("Failed to close JDBC connection: " + e.getMessage());
            return -1;
        }
    }

    @Override
    public void setImageClient(ImageClient imageClient) {
        this.imageClient = imageClient;
    }

    @Override
    public void handleClick(ActiveTab who, Integer x, Integer y) {
        if(conn!=null){
            Platform.runLater(()->{
                String path = FileUtils.analyticsPathForTab(who);
                String tableName = path.substring(path.lastIndexOf('_') + 1);
                if(imageClient==null){
                    logger.warning("No ImageClient set for SQLConnection, defaulting to filesystem");
                    imageClient = FilesystemImageClient.getInstance();
                }
                if(!imageClient.imageExists(URI.create(path))){
                    imageClient.uploadImage(URI.create(path), FileUtils.getSnapshot(who));

                }

                //create table if it doesn't exist
                try{
                    Statement stmt = conn.createStatement();
                    stmt.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (x INT, y INT, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                    stmt.close();
                } catch(SQLException e){
                    logger.warning("Failed to create table for " + tableName + ": " + e.getMessage());
                }

                //insert click into table
                try{
                    PreparedStatement stmt = conn.prepareStatement("INSERT INTO " + tableName + " (x, y) VALUES (?, ?)");
                    stmt.setInt(1, x);
                    stmt.setInt(2, y);
                    stmt.execute();
                    stmt.close();
                    logger.fine("Inserted click into table " + tableName);
                } catch(SQLException e){
                    logger.warning("Failed to insert click into table for " + tableName + ": " + e.getMessage());
                }
            });
        }
    }
}
