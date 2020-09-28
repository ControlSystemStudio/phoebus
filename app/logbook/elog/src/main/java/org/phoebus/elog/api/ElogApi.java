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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.CleanerProperties;

import net.dongliu.commons.collection.Lists;
import net.dongliu.requests.Requests;
import net.dongliu.requests.body.Part;
import net.dongliu.requests.Response;

import org.phoebus.logbook.LogbookException;

/**
 * PSI Elog API
 *
 * This class is based on the Python Library from PSI
 * https://github.com/paulscherrerinstitute/py_elog/blob/master/elog/logbook.py
 *
 * @author ffeldbauer
 */
public class ElogApi {

  private final String username;
  private final String password;
  private final String url;
  private final String logbook;


  public ElogApi( URI elogURI, String username, String password ) {
    String tmp = elogURI.toString();
    if( !tmp.endsWith("/") ) {
      this.url = tmp + "/";
    } else {
      this.url = tmp;
    }
    String path[] = elogURI.getPath().split("/");
    this.logbook  = path[ path.length-1 ];
    this.username = username;
    this.password = password;
  }


  /**
   * Posts message to the logbook.
   *
   * @param attributes
   */
  public long post( Map<String, String> attributes ) throws LogbookException {
    return post( attributes, Collections.emptyList(), Long.valueOf(-1) );
  }


  /**
   * Posts message to the logbook.
   *
   * @param attributes
   * @param attachments
   */
  public long post( Map<String, String> attributes, List<File> attachments ) throws LogbookException {
    return post( attributes, attachments, Long.valueOf(-1) );
  }


  /**
   * Posts message to the logbook.
   *
   * If msg_id is not specified new message will be created, otherwise existing
   * message will be edited. This method returns the msg_id of the newly created message.
   *
   * @param attributes
   * @param attachments
   * @param msgId
   * @return id of log entry
   */
  public long post( Map<String, String> attributes, List<File> attachments, Long msgId ) throws LogbookException {

    Map<String, String> new_attributes = new HashMap<>();
    new_attributes.putAll( attributes );
    new_attributes.put( "Encoding", "plain" );

    Map<String, String> attributes_to_edit = null;
    if( msgId > 0 ) {

      new_attributes.put( "edit_id", String.valueOf(msgId) );
      new_attributes.put( "skiplock", "1" );

      ElogEntry entry = read(msgId);

      attributes_to_edit = entry.getAttributes();

      int i = 0;
      for( String attach: entry.getAttachments() ) {
        new_attributes.put( "attachment" + String.valueOf(i), attach );
        i++;
      }

      for( Map.Entry<String, String> attr : new_attributes.entrySet() ) {
        if( !attr.getValue().isEmpty() ) {
          attributes_to_edit.put( attr.getKey(), attr.getValue() );
        }
      }

    } else {
      new_attributes.put( "When", String.valueOf( Instant.now().toEpochMilli() ));
    }

    if( attributes_to_edit == null ) {
      attributes_to_edit = new_attributes;
    }

    attributes_to_edit.remove("$@MID@$");
    attributes_to_edit.remove("Date");
    attributes_to_edit.remove("Attachment");

    List<Part<?>> data = new ArrayList<Part<?>>();
    for( Map.Entry<String, String> attr : attributes_to_edit.entrySet() ) {
      data.add( Part.text( attr.getKey(), attr.getValue() ));
    }
    data.add( Part.text( "cmd", "Submit" ));
    data.add( Part.text( "exp", this.logbook ));
    data.add( Part.text( "unm", this.username ));
    data.add( Part.text( "upwd", this.password ));
    data.add( Part.text( "Author", this.username ));

    int i = 0;
    for( File att: attachments ) {
      data.add( Part.file( "attfile" + String.valueOf(i), att ));
      i++;
    }

    Response<String> resp = Requests.post( this.url )
                                    .multiPartBody( data )
                                    .followRedirect(false)
                                    .verify(false)
                                    .send()
                                    .toTextResponse();
    return validateResponse( resp );

  }


  /**
   * Reads message from the logbook server
   *
   * @param msgId
   * @return {@link ElogEntry}
   */
  public ElogEntry read( Long msgId ) throws LogbookException {
    checkIfMessageOnServer( msgId );

    Map<String, Object> cookies = new HashMap<>();
    cookies.put( "unm", this.username );
    cookies.put( "upwd", this.password );

    Response<String> resp = Requests.get( this.url + String.valueOf( msgId ) + "?cmd=download" )
                                    .cookies(cookies)
                                    .followRedirect(false)
                                    .verify(false)
                                    .send()
                                    .toTextResponse();
    validateResponse( resp );

    List<String> returned_msg = Arrays.asList( resp.getBody().split("\\r?\\n") );
    int delimiter_idx = returned_msg.indexOf("========================================");

    Map<String, String> attributes = new HashMap<>();
    List<String> attachments = null;

    for( String s : returned_msg.subList( 0, delimiter_idx ) ) {
      String data[] = s.split( "\\s*:\\s*", 2 );

      if( data[0].equals("Attachment") ) {
        if( data[1].length() > 0 ) {
          attachments = Arrays.asList( data[1].split( "\\s*,\\s*" ) );
        } else {
          attachments = Collections.emptyList();
        }
      } else {
        attributes.put( data[0], data[1] );
      }
    }
    attributes.put( "Text", String.join("\n", returned_msg.subList( delimiter_idx + 1, returned_msg.size() ) ) );

    return new ElogEntry( attributes, attachments );
  }


  /**
   * Deletes message thread (!!!message + all replies!!!) from logbook.
   *
   * @param msgId
   */
  public void delete( Long msgId ) throws LogbookException {
    checkIfMessageOnServer( msgId );

    Map<String, Object> cookies = new HashMap<>();
    cookies.put( "unm", this.username );
    cookies.put( "upwd", this.password );

    Response<String> resp = Requests.get( this.url + String.valueOf( msgId ) + "?cmd=Delete&confirm=Yes" )
                                    .cookies(cookies)
                                    .followRedirect(false)
                                    .verify(false)
                                    .send()
                                    .toTextResponse();
    validateResponse( resp );
    if( resp.statusCode() == 200 ) {
      throw new LogbookException("Cannot process delete command (only logbooks in English supported).");
    }
  }


  /**
   * Searches the logbook and returns the message ids.
   *
   * @param search_term
   * @param {@link ElogEntry}
   */
  public List<ElogEntry> search( Map<String, String> search_term ) throws LogbookException {

    Map<String, Object> params = new HashMap<>();
    params.put( "mode", "summary" );
    params.put( "reverse", "1" );
    params.put( "npp", 20 ); // number of returned results...
    params.putAll( search_term );

    Map<String, Object> cookies = new HashMap<>();
    cookies.put( "unm", this.username );
    cookies.put( "upwd", this.password );

    Response<String> resp = Requests.get( this.url )
                                    .cookies(cookies)
                                    .params(params)
                                    .followRedirect(false)
                                    .verify(false)
                                    .send()
                                    .toTextResponse();
    validateResponse( resp );

    List<ElogEntry> entries = new ArrayList<ElogEntry>();

    try {
      TagNode tagNode = new HtmlCleaner().clean( resp.getBody() );
      org.w3c.dom.Document doc = new DomSerializer( new CleanerProperties()).createDOM(tagNode);
      XPath xpath = XPathFactory.newInstance().newXPath();
      NodeList msgIds = (NodeList) xpath.evaluate( "(//tr/td[@class=\"list1\" or @class=\"list2\"][1])/a/@href", doc, XPathConstants.NODESET );
      for( int i = 0; i < msgIds.getLength(); i++ ) {
        String msgIdStr = msgIds.item(i).getNodeValue();
        entries.add( read( Long.valueOf( msgIdStr.substring( msgIdStr.lastIndexOf('/') + 1 ) )));
      }
    } catch (XPathExpressionException e) {
      throw new LogbookException( e.getMessage() );
    } catch (ParserConfigurationException e) {
      throw new LogbookException( e.getMessage() );
    }

    return entries;
  }


  /**
   * Get a list of all messages on logbook
   *
   */
  public List<ElogEntry> getMessages() throws LogbookException {

    Map<String, Object> cookies = new HashMap<>();
    cookies.put( "unm", this.username );
    cookies.put( "upwd", this.password );

    Response<String> resp = Requests.get( this.url + "page?mode=summary" )
                                    .cookies(cookies)
                                    .followRedirect(false)
                                    .verify(false)
                                    .send()
                                    .toTextResponse();
    validateResponse( resp );

    List<ElogEntry> entries = new ArrayList<ElogEntry>();

    try {
      TagNode tagNode = new HtmlCleaner().clean( resp.getBody() );
      org.w3c.dom.Document doc = new DomSerializer( new CleanerProperties()).createDOM(tagNode);
      XPath xpath = XPathFactory.newInstance().newXPath();
      NodeList msgIds = (NodeList) xpath.evaluate( "(//tr/td[@class=\"list1\" or @class=\"list2\"][1])/a/@href", doc, XPathConstants.NODESET );
      for( int i = 0; i < msgIds.getLength(); i++ ) {
        String msgIdStr = msgIds.item(i).getNodeValue();
        entries.add( read( Long.valueOf( msgIdStr.substring( msgIdStr.lastIndexOf('/') + 1 ) )));
      }
    } catch (XPathExpressionException e) {
      throw new LogbookException( e.getMessage() );
    } catch (ParserConfigurationException e) {
      throw new LogbookException( e.getMessage() );
    }

    return entries;
  }


  /**
   * Get Attachment from Logbook
   *
   * @param filename
   * @return File
   */
  public File getAttachment( String filename ) throws LogbookException {
    Map<String, Object> cookies = new HashMap<>();
    cookies.put( "unm", this.username );
    cookies.put( "upwd", this.password );

    File att = new File("/tmp/" + filename );
    Requests.get( this.url + filename ).cookies( cookies ).followRedirect(false).verify(false).send().writeToFile(att);
    return att;
  }

  /**
   * Get list of available types
   *
   * @return Collection<String>
   */
  public Collection<String> getTypes() throws LogbookException {
    Map<String, Object> cookies = new HashMap<>();
    cookies.put( "unm", this.username );
    cookies.put( "upwd", this.password );

    Response<String> resp = Requests.get( this.url + "?cmd=new" )
                                    .cookies(cookies)
                                    .followRedirect(false)
                                    .verify(false)
                                    .send()
                                    .toTextResponse();
    //validateResponse( resp );
    //validateResponse does not work here, as the response body contains
    // "form name=form1"....
    int statuscode  = resp.statusCode();
    if( statuscode != 200 && statuscode != 302 ) {
      final Pattern pattern = Pattern.compile("<td.*?class=\"errormsg\".*?>(.*?)</td>");
      Matcher m = pattern.matcher( resp.getBody() );
      if( m.matches() ) {
        String err = m.group();
        throw new LogbookException("Rejected because of: " + err);
      } else {
        throw new LogbookException("Rejected because of unknown error.");
      }
    } else {
      String location = resp.getHeader( "Location" );
      if( location != null && !location.isEmpty() ) {
        if( location.contains( "has moved" )) {
          throw new LogbookException("Logbook server has moved to another location.");
        } else if( location.contains( "fail" )) {
          throw new LogbookException("Failed to submit log entry, invalid credentials.");
        }
      }
    }

    List<String> types = new ArrayList<String>();

    try {
      TagNode tagNode = new HtmlCleaner().clean( resp.getBody() );
      org.w3c.dom.Document doc = new DomSerializer( new CleanerProperties()).createDOM(tagNode);
      XPath xpath = XPathFactory.newInstance().newXPath();
      NodeList typenodes = (NodeList) xpath.evaluate( "//tr/td[@class=\"attribvalue\"]/select[@name=\"Type\"]/option/@value", doc, XPathConstants.NODESET );
      for( int i = 0; i < typenodes.getLength(); i++ ) {
        String t = typenodes.item(i).getNodeValue();
        if( !t.isEmpty() ) {
          types.add( t );
        }
      }
    } catch (XPathExpressionException e) {
      throw new LogbookException( e.getMessage() );
    } catch (ParserConfigurationException e) {
      throw new LogbookException( e.getMessage() );
    }

    return types;
  }

  /**
   * Get list of available categories
   *
   * @return Collection<String>
   */
  public Collection<String> getCategories() throws LogbookException {
    Map<String, Object> cookies = new HashMap<>();
    cookies.put( "unm", this.username );
    cookies.put( "upwd", this.password );

    Response<String> resp = Requests.get( this.url + "?cmd=new" )
                                    .cookies(cookies)
                                    .followRedirect(false)
                                    .verify(false)
                                    .send()
                                    .toTextResponse();
    //validateResponse( resp );
    //validateResponse does not work here, as the response body contains
    // "form name=form1"....
    int statuscode  = resp.statusCode();
    if( statuscode != 200 && statuscode != 302 ) {
      final Pattern pattern = Pattern.compile("<td.*?class=\"errormsg\".*?>(.*?)</td>");
      Matcher m = pattern.matcher( resp.getBody() );
      if( m.matches() ) {
        String err = m.group();
        throw new LogbookException("Rejected because of: " + err);
      } else {
        throw new LogbookException("Rejected because of unknown error.");
      }
    } else {
      String location = resp.getHeader( "Location" );
      if( location != null && !location.isEmpty() ) {
        if( location.contains( "has moved" )) {
          throw new LogbookException("Logbook server has moved to another location.");
        } else if( location.contains( "fail" )) {
          throw new LogbookException("Failed to submit log entry, invalid credentials.");
        }
      }
    }

    List<String> categories = new ArrayList<String>();

    try {
      TagNode tagNode = new HtmlCleaner().clean( resp.getBody() );
      org.w3c.dom.Document doc = new DomSerializer( new CleanerProperties()).createDOM(tagNode);
      XPath xpath = XPathFactory.newInstance().newXPath();
      NodeList catnodes = (NodeList) xpath.evaluate( "//tr/td[@class=\"attribvalue\"]/select[@name=\"Category\"]/option/@value", doc, XPathConstants.NODESET );
      for( int i = 0; i < catnodes.getLength(); i++ ) {
        String c = catnodes.item(i).getNodeValue();
        if( !c.isEmpty() ) {
          categories.add( c );
        }
      }
    } catch (XPathExpressionException e) {
      throw new LogbookException( e.getMessage() );
    } catch (ParserConfigurationException e) {
      throw new LogbookException( e.getMessage() );
    }

    return categories;
  }




  /**
   * Try to load page for specific message.
   *
   * @param logId ID of message to be checked
   */
  private void checkIfMessageOnServer( Long logId ) throws LogbookException {
    Map<String, Object> cookies = new HashMap<>();
    cookies.put( "unm", this.username );
    cookies.put( "upwd", this.password );

    Response<String> check = Requests.get( this.url + String.valueOf( logId ) )
                                     .cookies(cookies)
                                     .followRedirect(false)
                                     .verify(false)
                                     .send()
                                     .toTextResponse();
    validateResponse( check );
    if( Pattern.matches( "<td.*?class=\"errormsg\".*?>.*?</td>", check.getBody() )) {
      throw new LogbookException("Message with ID " + String.valueOf(logId) + " does not exist on logbook.");
    }
  }


  /**
   * Validate response of the request.
   *
   * @param resp
   * @return id of new message
   */
  private long validateResponse( Response<String> resp ) throws LogbookException {
    int statuscode  = resp.statusCode();
    String body = resp.getBody();
    long msgId = -1;
    if( statuscode != 200 && statuscode != 302 ) {
      final Pattern pattern = Pattern.compile("<td.*?class=\"errormsg\".*?>(.*?)</td>");
      Matcher m = pattern.matcher( body );
      if( m.matches() ) {
        String err = m.group();
        throw new LogbookException("Rejected because of: " + err);
      } else {
        throw new LogbookException("Rejected because of unknown error.");
      }
    } else {
      String location = resp.getHeader( "Location" );
      if( location != null && !location.isEmpty() ) {
        if( location.contains( "has moved" )) {
          throw new LogbookException("Logbook server has moved to another location.");
        } else if( location.contains( "fail" )) {
          throw new LogbookException("Failed to submit log entry, invalid credentials.");
        } else {
          URI loc = URI.create( location );
          String path[] = loc.getPath().split("/");
          msgId = Integer.parseInt( path[ path.length-1 ] );
        }
      }
      if( body.contains("form name=form1") || body.contains("type=password") ) {
        throw new LogbookException("Failed to submit log entry, invalid credentials.");
      }
    }
    return msgId;
  }


}

