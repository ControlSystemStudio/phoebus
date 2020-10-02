package org.phoebus.elog.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

 /**
 * PSI Elog Entry
 *
 * @author ffeldbauer
 */
public class ElogEntry {
  private final Map<String, String> attributes;
  private final List<String> attachments;


  public ElogEntry( Map<String, String> attributes, List<String> attachments ) {
    this.attributes = attributes;
    this.attachments = attachments;
  }


  public Map<String, String> getAttributes() {
    return this.attributes;
  }


  public List<String> getAttachments() {
    return this.attachments;
  }


  public String getAttribute( String key ) {
    return this.attributes.get( key );
  }
}

