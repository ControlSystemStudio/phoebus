package org.phoebus.elog.api;

import net.dongliu.requests.Requests;
import net.dongliu.requests.Response;
import net.dongliu.requests.body.Part;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.phoebus.logbook.LogbookException;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PSI Elog API
 *
 * This class is based on the Python Library from PSI
 * https://github.com/paulscherrerinstitute/py_elog/blob/master/elog/logbook.py
 *
 * @author ffeldbauer
 * @author kingspride
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
    String[] path = elogURI.getPath().split("/");
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
    return post( attributes, Collections.emptyList(), -1L);
  }


  /**
   * Posts message to the logbook.
   *
   * @param attributes
   * @param attachments
   */
  public long post( Map<String, String> attributes, List<File> attachments ) throws LogbookException {
    return post( attributes, attachments, -1L);
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

    String encoded_Text = new String(attributes.get("Text").getBytes(), StandardCharsets.ISO_8859_1); // charset hell - this seems stupid but works
    attributes.put("Text", encoded_Text);
    String encoded_Subject = new String(attributes.get("Subject").getBytes(), StandardCharsets.UTF_8); // charset hell - this seems stupid but works
    attributes.put("Subject", encoded_Subject);

    Map<String, String> new_attributes = new HashMap<>(attributes);
    new_attributes.put( "Encoding", "plain" );

    Map<String, String> attributes_to_edit = null;
    if( msgId > 0 ) {

      new_attributes.put( "edit_id", String.valueOf(msgId) );
      new_attributes.put( "skiplock", "1" );

      ElogEntry entry = read(msgId);

      attributes_to_edit = entry.getAttributes();

      int i = 0;
      for( String attach: entry.getAttachments() ) {
        new_attributes.put( "attachment" + i, attach );
        i++;
      }

      for( Map.Entry<String, String> attr : new_attributes.entrySet() ) {
        if( !attr.getValue().isEmpty() ) {
          attributes_to_edit.put( attr.getKey(), attr.getValue() );
        }
      }

    } else {
      new_attributes.put( "When", String.valueOf( Instant.now().toEpochMilli() ));
      new_attributes.put( "Record_date", String.valueOf( Instant.now().getEpochSecond() ));
    }

    if( attributes_to_edit == null ) {
      attributes_to_edit = new_attributes;
    }

    attributes_to_edit.remove("$@MID@$");
    attributes_to_edit.remove("Date");
    attributes_to_edit.remove("Attachment");

    List<Part<?>> data = new ArrayList<>();
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
      data.add( Part.file( "attfile" + i, att ));
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

    Response<String> resp = Requests.get( this.url + msgId + "?cmd=download" )
                                    .cookies(cookies)
                                    .followRedirect(false)
                                    .verify(false)
                                    .send()
                                    .toTextResponse();
    validateResponse( resp );

    List<String> returned_msg = Arrays.asList( resp.body().split("\\r?\\n") );
    int delimiter_idx = returned_msg.indexOf("========================================");

    Map<String, String> attributes = new HashMap<>();
    List<String> attachments = null;

    for( String s : returned_msg.subList( 0, delimiter_idx ) ) {
      String[] data = s.split( "\\s*:\\s*", 2 );

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

    Response<String> resp = Requests.get( this.url + msgId + "?cmd=Delete&confirm=Yes" )
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
   * 
   */
  public ElogSearchResult search( Map<String, String> search_term, Integer from, Integer size ) throws LogbookException {

    Map<String, Object> params = new HashMap<>();
    params.put( "mode", "summary" );

    // Sort by date
    if(search_term.containsKey("sort")) {
        String sortDirection = search_term.get("sort");
        if("up".equalsIgnoreCase(sortDirection)) {
            params.put( "reverse", "0" );
        } else if("down".equalsIgnoreCase(sortDirection)) {
            params.put( "reverse", "1" );
        } 
        search_term.remove("sort");
    } else {
        params.put( "reverse", "1" );
    }

    // Pagination parameters
    String requestUrl = this.url;
    if(from != null && size != null) {
        if(size != 0) {
            int page = from / size + 1;
            requestUrl = String.format("%spage%d", requestUrl, page);
            params.put( "npp", size );
        }
    } else {
        params.put( "npp", 20 ); // number of returned results...
    }

    params.putAll( search_term );

    Map<String, Object> cookies = new HashMap<>();
    cookies.put( "unm", this.username );
    cookies.put( "upwd", this.password );

    Response<String> resp = Requests.get( requestUrl )
                                    .cookies(cookies)
                                    .params(params)
                                    .followRedirect(false)
                                    .verify(false)
                                    .send()
                                    .toTextResponse();
    validateResponse( resp );

    List<ElogEntry> entries = new ArrayList<>();
    int totalCount = 0;

    try {
      TagNode tagNode = new HtmlCleaner().clean( resp.body() );
      org.w3c.dom.Document doc = new DomSerializer( new CleanerProperties()).createDOM(tagNode);
      XPath xpath = XPathFactory.newInstance().newXPath();
      NodeList msgIds = (NodeList) xpath.evaluate( "(//tr/td[@class=\"list1\" or @class=\"list2\"][1])/a/@href", doc, XPathConstants.NODESET );
      
      // Extract the number of all entries
      String expression = "//b[contains(., 'Entries')]";
      String result = (String) xpath.evaluate(expression, doc, XPathConstants.STRING);
      if(result != null && !result.isEmpty()) {
          java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s+Entries");
          java.util.regex.Matcher matcher = pattern.matcher(result);
          if(matcher.find()) {
              totalCount = Integer.parseInt(matcher.group(1));
          }
      }
      
      for( int i = 0; i < msgIds.getLength(); i++ ) {
        String msgIdStr = msgIds.item(i).getNodeValue();
        entries.add( read( Long.valueOf( msgIdStr.substring( msgIdStr.lastIndexOf('/') + 1 ) )));
      }
    } catch (XPathExpressionException | ParserConfigurationException e) {
      throw new LogbookException( "could not parse the elog response", e );
    }

    return ElogSearchResult.of(entries, totalCount);
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

    List<ElogEntry> entries = new ArrayList<>();

    try {
      TagNode tagNode = new HtmlCleaner().clean( resp.body() );
      org.w3c.dom.Document doc = new DomSerializer( new CleanerProperties()).createDOM(tagNode);
      XPath xpath = XPathFactory.newInstance().newXPath();
      NodeList msgIds = (NodeList) xpath.evaluate( "(//tr/td[@class=\"list1\" or @class=\"list2\"][1])/a/@href", doc, XPathConstants.NODESET );
      for( int i = 0; i < msgIds.getLength(); i++ ) {
        String msgIdStr = msgIds.item(i).getNodeValue();
        entries.add( read( Long.valueOf( msgIdStr.substring( msgIdStr.lastIndexOf('/') + 1 ) )));
      }
    } catch (XPathExpressionException | ParserConfigurationException e) {
      throw new LogbookException( "could not parse the elog response", e );
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

    String tmpdir = System.getProperty("java.io.tmpdir");
    File att = new File(tmpdir + "/" + filename );
    Requests.get( this.url + filename ).cookies( cookies ).followRedirect(false).verify(false).send().writeToFile(att);
    return att;
  }

  /**
   * Get list of available types
   *
   * @return {@literal Collection<String>}
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
      Matcher m = pattern.matcher( resp.body() );
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

    List<String> types = new ArrayList<>();

    try {
      TagNode tagNode = new HtmlCleaner().clean( resp.body() );
      org.w3c.dom.Document doc = new DomSerializer( new CleanerProperties()).createDOM(tagNode);
      XPath xpath = XPathFactory.newInstance().newXPath();
      NodeList typenodes = (NodeList) xpath.evaluate( "//tr/td[@class=\"attribvalue\"]/select[@name=\"Type\"]/option/@value", doc, XPathConstants.NODESET );
      for( int i = 0; i < typenodes.getLength(); i++ ) {
        String t = typenodes.item(i).getNodeValue();
        if( !t.isEmpty() ) {
          types.add( t );
        }
      }
    } catch (XPathExpressionException | ParserConfigurationException e) {
      throw new LogbookException( "could not parse the elog response", e );
    }

    return types;
  }

  /**
   * Get list of available categories
   *
   * @return {@literal Collection<String>}
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
      Matcher m = pattern.matcher( resp.body() );
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

    List<String> categories = new ArrayList<>();

    try {
      TagNode tagNode = new HtmlCleaner().clean( resp.body() );
      org.w3c.dom.Document doc = new DomSerializer( new CleanerProperties()).createDOM(tagNode);
      XPath xpath = XPathFactory.newInstance().newXPath();
      NodeList catnodes = (NodeList) xpath.evaluate( "//tr/td[@class=\"attribvalue\"]/select[@name=\"Category\"]/option/@value", doc, XPathConstants.NODESET );
      for( int i = 0; i < catnodes.getLength(); i++ ) {
        String c = catnodes.item(i).getNodeValue();
        if( !c.isEmpty() ) {
          categories.add( c );
        }
      }
    } catch (XPathExpressionException | ParserConfigurationException e) {
      throw new LogbookException( "could not parse the elog response", e );
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

    Response<String> check = Requests.get( this.url + logId)
                                     .cookies(cookies)
                                     .followRedirect(false)
                                     .verify(false)
                                     .send()
                                     .toTextResponse();
    validateResponse( check );
    if( Pattern.matches( "<td.*?class=\"errormsg\".*?>.*?</td>", check.body() )) {
      throw new LogbookException("Message with ID " + logId + " does not exist on logbook.");
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
    String body = resp.body();
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
          String[] path = loc.getPath().split("/");
          String msgIdStr = path[ path.length-1 ];
          if(msgIdStr != null && msgIdStr.matches("\\d+")) {
            msgId = Integer.parseInt( msgIdStr );
          }
        }
      }
      if( body.contains("form name=form1") || body.contains("type=password") ) {
        throw new LogbookException("Failed to submit log entry, invalid credentials.");
      }
    }
    return msgId;
  }


}

