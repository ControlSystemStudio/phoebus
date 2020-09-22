package org.phoebus.elog.api;

import java.time.Instant;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.net.URI;
import java.net.FileNameMap;
import java.net.URLConnection;

import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.AttachmentImpl;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookImpl;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.Messages;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.PropertyImpl;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.TagImpl;

/**
 * A logbook client to the Elog logbook service
 *
 * @author ffeldbauer
 *
 */
public class ElogClient implements LogClient{
    private final ElogApi service;
    private Collection<Tag> categories;
    private Collection<Logbook> types;
    private static FileNameMap fileNameMap = URLConnection.getFileNameMap();

    /**
     * Builder Class to help create a elog client.
     * 
     * @author ffeldbauer
     * 
     */
    public static class ElogClientBuilder {
        private URI elogURI = null;
        private String username = null;
        private String password = null;
        private ElogProperties properties = new ElogProperties();

        private ElogClientBuilder() {
            this.elogURI = URI.create( this.properties.getPreferenceValue("elog_url") );
        }

        /**
         * Creates a {@link ElogClientBuilder} for a CF client to Default URL in the
         * channelfinder.properties.
         * 
         * @return
         */
        public static ElogClientBuilder serviceURL() {
            return new ElogClientBuilder();
        }

        /**
         * Set the username to be used for Authentication.
         * 
         * @param username
         * @return {@link ElogClientBuilder}
         */
        public ElogClientBuilder username(String username) {
            this.username = username;
            return this;
        }

        /**
         * Set the password to be used for the Authentication.
         * 
         * @param password
         * @return {@link ElogClientBuilder}
         */
        public ElogClientBuilder password(String password) {
            char[] ch = new char[password.length()];
            for (int i = 0; i < password.length(); i++) {
                ch[i] = password.charAt(i);
            }
            this.password = Sha256.sha256( ch, 5000 );
            return this;
        }

        /**
         * Create new instance of <code>ElogClient</code>
         * 
         * @return {@link ElogClient}
         */
        public ElogClient create() throws Exception {
            this.username = ifNullReturnPreferenceValue(this.username, "username");
            this.password = ifNullReturnPreferenceValue(this.password, "password");

            List<Logbook> types = null;
            String types_prop = this.properties.getPreferenceValue("types");
            if( !types_prop.isEmpty() ) {
                types = new ArrayList<Logbook>();
                for( String s: types_prop.split("\\s*,\\s*") ){
                    types.add( LogbookImpl.of( s ) );
                }
            }

            List<Tag> categories = null;
            String categories_prop = this.properties.getPreferenceValue("categories");
            if( !categories_prop.isEmpty() ) {
                categories = new ArrayList<Tag>();
                for( String s: categories_prop.split("\\s*,\\s*") ){
                    categories.add( TagImpl.of( s ) );
                }
            }
            ElogApi service = new ElogApi( this.elogURI, this.username, this.password );
            return new ElogClient( service, categories, types );
        }


        private String ifNullReturnPreferenceValue(String value, String key) {
            if (value == null) {
                return this.properties.getPreferenceValue(key);
            } else {
                return value;
            }
        }
    }


    private ElogClient( ElogApi service, Collection<Tag> categories, Collection<Logbook> types ) {
        this.service = service;
        if( types == null ) {
            this.types = new ArrayList<Logbook>();
            try {
                for( String t : service.getTypes() ) {
                    this.types.add( LogbookImpl.of( t ));
                }
            } catch(LogbookException e){
                e.printStackTrace();
            }
        } else {
            this.types = types;
        }
        if( categories == null ) {
            this.categories = new ArrayList<Tag>();
            try {
                for( String c : service.getCategories() ) {
                    this.categories.add( TagImpl.of( c ));
                }
            } catch(LogbookException e){
                e.printStackTrace();
            }
        } else {
            this.categories = categories;
        }
    }


    @Override
    public LogEntry set(LogEntry log) throws LogbookException {
        Map<String, String> attributes = new HashMap<>();

        for( Logbook l: log.getLogbooks() ) {
            if( !l.getName().isEmpty() ) {
                attributes.put( "Type", l.getName() );
                break;
            }
        }
        if( !attributes.containsKey("Type") ) {
            Logger.getLogger( ElogClient.class.getPackageName()).severe( "No valid type selected. Cannot submit log entry" );
            return null;
        }

        for( Tag t: log.getTags() ) {
            if( !t.getName().isEmpty() ) {
                attributes.put( "Category", t.getName() );
                break;
            }
        }

        attributes.put( "Subject", log.getTitle() );
        attributes.put( "Text", log.getDescription() );

        List<File> files = new ArrayList<File>();
        for( Attachment att: log.getAttachments() ) {
          files.add( att.getFile() );
        }

        long msgId = service.post( attributes, files );

        LogEntryBuilder logBuilder = LogEntryImpl.LogEntryBuilder.log( log );
        logBuilder.id( msgId );
        logBuilder.createdDate( Instant.now() );
        logBuilder.modifiedDate( Instant.now() );
        LogEntry logEntry = logBuilder.build();
        return logEntry;
    }


    @Override
    public LogEntry getLog(Long logId) {
        ElogEntry entry = null;
        try{
            entry = service.read( logId );
        } catch(LogbookException e){
            e.printStackTrace();
        }

        LogEntryBuilder logBuilder = LogEntryImpl.LogEntryBuilder.log();
        logBuilder.id( logId );
        logBuilder.description( entry.getAttribute("Text") );
        logBuilder.title( entry.getAttribute("Subject") );
        try {
            Date date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ").parse( entry.getAttribute("Date") );
            logBuilder.createdDate( date.toInstant() );
            logBuilder.modifiedDate( date.toInstant() );
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        logBuilder.appendTag( TagImpl.of( entry.getAttribute("Category") ));
        logBuilder.appendToLogbook( LogbookImpl.of( entry.getAttribute("Type") ));

        try{
            for( String s : entry.getAttachments() ) {
                String mimeType = fileNameMap.getContentTypeFor(s);
                if( mimeType == null ) {
                    if( s.endsWith(".py") ){
                        logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "text/x-python", false ));
                    } else if( s.endsWith(".pyc") ){
                        logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "application/x-python-code", false ));
                    } else {
                        logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "unkown", false ));
                    }
                } else {
                    logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), mimeType, false ));
                }
            }
        } catch(LogbookException e){
            e.printStackTrace();
        } catch(FileNotFoundException e){
            e.printStackTrace();
        }

        LogEntry logEntry = logBuilder.build();
        return logEntry;
    }


    @Override
    public Collection<Attachment> listAttachments(Long logId) {
        List<Attachment> attlist = new ArrayList<Attachment>();
        try{
            ElogEntry entry = service.read( logId );
            for( String s : entry.getAttachments() ) {
                String mimeType = fileNameMap.getContentTypeFor(s);
                if( mimeType == null ) {
                    if( s.endsWith(".py") ){
                        attlist.add( AttachmentImpl.of( service.getAttachment(s), "text/x-python", false ));
                    } else if( s.endsWith(".pyc") ){
                        attlist.add( AttachmentImpl.of( service.getAttachment(s), "application/x-python-code", false ));
                    } else {
                        attlist.add( AttachmentImpl.of( service.getAttachment(s), "unkown", false ));
                    }
                } else {
                    attlist.add( AttachmentImpl.of( service.getAttachment(s), mimeType, false ));
                }
            }
        } catch(LogbookException e){
            e.printStackTrace();
        } catch(FileNotFoundException e){
            e.printStackTrace();
        }
        return attlist;
    }


    @Override
    public List<LogEntry> findLogs(Map<String, String> map) {
        Map<String, String> query = new HashMap<>();
        query.putAll( map );

        if( query.containsKey( "start" ) ) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse( map.get( "start" ) );
                query.put("ma", String.valueOf( date.getMonth() + 1 ));
                query.put("da", String.valueOf( date.getDate() ));
                query.put("ya", String.valueOf( date.getYear() + 1900 ));
                query.put("ha", String.valueOf( date.getHours() ));
                query.put("na", String.valueOf( date.getMinutes() ));
                query.put("ca", String.valueOf( date.getSeconds() ));
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
            query.remove("start");
        }
        if( query.containsKey( "end" ) ) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse( map.get( "end" ) );
                query.put("mb", String.valueOf( date.getMonth() + 1 ));
                query.put("db", String.valueOf( date.getDate() ));
                query.put("yb", String.valueOf( date.getYear() + 1900 ));
                query.put("hb", String.valueOf( date.getHours() ));
                query.put("nb", String.valueOf( date.getMinutes() ));
                query.put("cb", String.valueOf( date.getSeconds() ));
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
            query.remove("end");
        }
        if( query.containsKey( "desc" ) ) {
            String subtext = map.get( "desc" );
            if( !subtext.equals("*") ) {
                query.put("subtext", subtext );
            }
            query.remove("desc");
        }
        if( query.containsKey( "logbook" ) ) {
            String type = map.get( "logbook" );
            if( type.contains(",") ) {
                query.put("Type", type.substring( 0, type.indexOf(",") ) );
            } else {
                query.put("Type", type );
            }
            query.remove("logbook");
        }
        if( query.containsKey( "tag" ) ) {
            String category = map.get( "tag" );
            if( category.contains(",") ) {
                query.put("Category", category.substring( 0, category.indexOf(",") ) );
            } else {
                query.put("Category", category );
            }
            query.remove("tag");
        }

        List<LogEntry> entries = new ArrayList<LogEntry>();
        try {
            for( ElogEntry entry : service.search( query ) ) {
                LogEntryBuilder logBuilder = LogEntryImpl.LogEntryBuilder.log();
                logBuilder.id( Long.valueOf( entry.getAttribute("$@MID@$") ));
                logBuilder.description( entry.getAttribute("Text") );
                logBuilder.title( entry.getAttribute("Subject") );
                try {
                    Date date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ").parse( entry.getAttribute("Date") );
                    logBuilder.createdDate( date.toInstant() );
                    logBuilder.modifiedDate( date.toInstant() );
                } catch (ParseException e) {
                    e.printStackTrace();
                    return null;
                }
                logBuilder.appendTag( TagImpl.of( entry.getAttribute("Category") ));
                logBuilder.appendToLogbook( LogbookImpl.of( entry.getAttribute("Type") ));

                try {
                    for( String s : entry.getAttachments() ) {
                        String mimeType = fileNameMap.getContentTypeFor(s);
                        if( mimeType == null ) {
                            if( s.endsWith(".py") ){
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "text/x-python", false ));
                            } else if( s.endsWith(".pyc") ){
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "application/x-python-code", false ));
                            } else {
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "unkown", false ));
                            }
                        } else {
                            logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), mimeType, false ));
                        }
                    }
                } catch(FileNotFoundException e){
                    e.printStackTrace();
                }

                entries.add( logBuilder.build() );
            }
        } catch(LogbookException e){
            e.printStackTrace();
        }
        return entries;
    }


    @Override
    public Collection<LogEntry> listLogs() {
          List<LogEntry> entries = new ArrayList<LogEntry>();
          try{
            for( ElogEntry entry : service.getMessages() ) {
                LogEntryBuilder logBuilder = LogEntryImpl.LogEntryBuilder.log();
                logBuilder.id( Long.valueOf( entry.getAttribute("$@MID@$") ));
                logBuilder.description( entry.getAttribute("Text") );
                logBuilder.title( entry.getAttribute("Subject") );
                try {
                    Date date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ").parse( entry.getAttribute("Date") );
                    logBuilder.createdDate( date.toInstant() );
                    logBuilder.modifiedDate( date.toInstant() );
                } catch (ParseException e) {
                    e.printStackTrace();
                    return null;
                }
                logBuilder.appendTag( TagImpl.of( entry.getAttribute("Category") ));
                logBuilder.appendToLogbook( LogbookImpl.of( entry.getAttribute("Type") ));

                try{
                    for( String s : entry.getAttachments() ) {
                        String mimeType = fileNameMap.getContentTypeFor(s);
                        if( mimeType == null ) {
                            if( s.endsWith(".py") ){
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "text/x-python", false ));
                            } else if( s.endsWith(".pyc") ){
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "application/x-python-code", false ));
                            } else {
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "unkown", false ));
                            }
                        } else {
                            logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), mimeType, false ));
                        }
                    }
                } catch(FileNotFoundException e){
                    e.printStackTrace();
                }

                entries.add( logBuilder.build() );
            }
        } catch(LogbookException e){
            e.printStackTrace();
        }
        return entries;
    }


    @Override
    public Collection<Logbook> listLogbooks() {
        return this.types;
    }


    @Override
    public Collection<Tag> listTags() {
        return this.categories;
    }


    @Override
    public InputStream getAttachment(Long logId, Attachment attachment) throws LogbookException {
        return getAttachment( logId, attachment.getName() );
    }


    @Override
    public InputStream getAttachment(Long logId, String attachmentName) throws LogbookException {
        ElogEntry entry = service.read( logId );
        try {
            for( String s : entry.getAttachments() ) {
                if( s.equals( attachmentName ) ) {
                    return new FileInputStream( service.getAttachment(s) );
                }
            }
        } catch( java.io.FileNotFoundException e ) {
            throw new LogbookException( e.getMessage() );
        }
        throw new LogbookException( "Message " + logId + " has no matching attachment" );
    }


    @Override
    public Tag set(Tag tag) throws LogbookException {
        // there is no HTTP request to add new Categories to the Elog.
        // This can only be done by posting a new entry with a new Category value
        // This only works if "Extendable Options = Category" is set in Elog config
        this.categories.add( tag );
        return tag;
        //throw new LogbookException(new UnsupportedOperationException());
    }


    @Override
    public Logbook set(Logbook Logbook) throws LogbookException {
        // there is no HTTP request to add new Types to the Elog.
        // This can only be done by posting a new entry with a new Type value
        // This only works if "Extendable Options = Type" is set in Elog config
        this.types.add( Logbook );
        return Logbook;
        //throw new LogbookException(new UnsupportedOperationException());
    }


    @Override
    public LogEntry update(LogEntry log) throws LogbookException {
        Map<String, String> attributes = new HashMap<>();

        for( Logbook l: log.getLogbooks() ) {
            if( !l.getName().isEmpty() ) {
                attributes.put( "Type", l.getName() );
                break;
            }
        }
        if( !attributes.containsKey("Type") ) {
            Logger.getLogger( ElogClient.class.getPackageName()).severe( "No valid type selected. Cannot submit log entry" );
            return null;
        }

        for( Tag t: log.getTags() ) {
            if( !t.getName().isEmpty() ) {
                attributes.put( "Category", t.getName() );
                break;
            }
        }

        attributes.put( "Subject", log.getTitle() );
        attributes.put( "Text", log.getDescription() );

        List<File> files = new ArrayList<File>();
        for( Attachment att: log.getAttachments() ) {
          files.add( att.getFile() );
        }

        long msgId = service.post( attributes, files, log.getId() );

        LogEntryBuilder logBuilder = LogEntryImpl.LogEntryBuilder.log( log );
        logBuilder.modifiedDate( Instant.now() );
        LogEntry logEntry = logBuilder.build();
        return logEntry;
    }

    @Override
    public Collection<LogEntry> update(Collection<LogEntry> logs) throws LogbookException {
        List<LogEntry> entries = new ArrayList<LogEntry>();
        for( LogEntry entry : logs ) {
            entries.add( this.update( entry ));
        }
        return entries;
    }


    @Override
    public LogEntry findLogById(Long logId) throws LogbookException {
        return getLog( logId );
    }


    @Override
    public List<LogEntry> findLogsBySearch(String pattern) {
        Map<String, String> query = new HashMap<>();
        query.put( "subtext", pattern );
        query.put( "sall", "1" );

        List<LogEntry> entries = new ArrayList<LogEntry>();
        try{
            for( ElogEntry entry : service.search( query ) ) {
                LogEntryBuilder logBuilder = LogEntryImpl.LogEntryBuilder.log();
                logBuilder.id( Long.valueOf( entry.getAttribute("$@MID@$") ));
                logBuilder.description( entry.getAttribute("Text") );
                logBuilder.title( entry.getAttribute("Subject") );
                try {
                    Date date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ").parse( entry.getAttribute("Date") );
                    logBuilder.createdDate( date.toInstant() );
                    logBuilder.modifiedDate( date.toInstant() );
                } catch (ParseException e) {
                    e.printStackTrace();
                    return null;
                }
                logBuilder.appendTag( TagImpl.of( entry.getAttribute("Category") ));
                logBuilder.appendToLogbook( LogbookImpl.of( entry.getAttribute("Type") ));

                try{
                    for( String s : entry.getAttachments() ) {
                        String mimeType = fileNameMap.getContentTypeFor(s);
                        if( mimeType == null ) {
                            if( s.endsWith(".py") ){
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "text/x-python", false ));
                            } else if( s.endsWith(".pyc") ){
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "application/x-python-code", false ));
                            } else {
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "unkown", false ));
                            }
                        } else {
                            logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), mimeType, false ));
                        }
                    }
                } catch(FileNotFoundException e){
                    e.printStackTrace();
                }

                entries.add( logBuilder.build() );
            }
        } catch(LogbookException e){
            e.printStackTrace();
        }
        return entries;
    }


    @Override
    public List<LogEntry> findLogsByTag(String pattern) {
        Map<String, String> query = new HashMap<>();
        query.put( "Category", pattern );

        List<LogEntry> entries = new ArrayList<LogEntry>();
        try{
            for( ElogEntry entry : service.search( query ) ) {
                LogEntryBuilder logBuilder = LogEntryImpl.LogEntryBuilder.log();
                logBuilder.id( Long.valueOf( entry.getAttribute("$@MID@$") ));
                logBuilder.description( entry.getAttribute("Text") );
                logBuilder.title( entry.getAttribute("Subject") );
                try {
                    Date date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ").parse( entry.getAttribute("Date") );
                    logBuilder.createdDate( date.toInstant() );
                    logBuilder.modifiedDate( date.toInstant() );
                } catch (ParseException e) {
                    e.printStackTrace();
                    return null;
                }
                logBuilder.appendTag( TagImpl.of( entry.getAttribute("Category") ));
                logBuilder.appendToLogbook( LogbookImpl.of( entry.getAttribute("Type") ));

                try{
                    for( String s : entry.getAttachments() ) {
                        String mimeType = fileNameMap.getContentTypeFor(s);
                        if( mimeType == null ) {
                            if( s.endsWith(".py") ){
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "text/x-python", false ));
                            } else if( s.endsWith(".pyc") ){
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "application/x-python-code", false ));
                            } else {
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "unkown", false ));
                            }
                        } else {
                            logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), mimeType, false ));
                        }
                    }
                } catch(FileNotFoundException e){
                    e.printStackTrace();
                }

                entries.add( logBuilder.build() );
            }
        } catch(LogbookException e){
            e.printStackTrace();
        }
        return entries;
    }


    @Override
    public List<LogEntry> findLogsByLogbook(String logbook) {
        Map<String, String> query = new HashMap<>();
        query.put( "Type", logbook );

        List<LogEntry> entries = new ArrayList<LogEntry>();
        try{
            for( ElogEntry entry : service.search( query ) ) {
                LogEntryBuilder logBuilder = LogEntryImpl.LogEntryBuilder.log();
                logBuilder.id( Long.valueOf( entry.getAttribute("$@MID@$") ));
                logBuilder.description( entry.getAttribute("Text") );
                logBuilder.title( entry.getAttribute("Subject") );
                try {
                    Date date = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss ZZZZ").parse( entry.getAttribute("Date") );
                    logBuilder.createdDate( date.toInstant() );
                    logBuilder.modifiedDate( date.toInstant() );
                } catch (ParseException e) {
                    e.printStackTrace();
                    return null;
                }
                logBuilder.appendTag( TagImpl.of( entry.getAttribute("Category") ));
                logBuilder.appendToLogbook( LogbookImpl.of( entry.getAttribute("Type") ));

                try{
                    for( String s : entry.getAttachments() ) {
                        String mimeType = fileNameMap.getContentTypeFor(s);
                        if( mimeType == null ) {
                            if( s.endsWith(".py") ){
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "text/x-python", false ));
                            } else if( s.endsWith(".pyc") ){
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "application/x-python-code", false ));
                            } else {
                                logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), "unkown", false ));
                            }
                        } else {
                            logBuilder.attach( AttachmentImpl.of( service.getAttachment(s), mimeType, false ));
                        }
                    }
                } catch(FileNotFoundException e){
                    e.printStackTrace();
                }

                entries.add( logBuilder.build() );
            }
        } catch(LogbookException e){
            e.printStackTrace();
        }
        return entries;
    }


    @Override
    public void delete(LogEntry log) throws LogbookException {
        service.delete( log.getId() );
    }


    @Override
    public void delete(Long logId) throws LogbookException {
        service.delete( logId );
    }


    @Override
    public void delete(Collection<LogEntry> logIds) throws LogbookException {
        for( LogEntry entry : logIds ) {
            service.delete( entry.getId() );
        }
    }

}

