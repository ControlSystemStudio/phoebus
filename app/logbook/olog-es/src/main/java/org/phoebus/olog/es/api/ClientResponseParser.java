/**
 * 
 */
package org.phoebus.olog.es.api;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;

/**
 * @author Eric Berryman taken from shroffk
 *  Parses the HTML pay load containing the details about the
 *         error TODO improve the parsing
 */
class ClientResponseParser extends ParserCallback {

	private String message = "";
	boolean messageFlag = false;
	boolean causeFlag = false;

	public ClientResponseParser() {
		super();
	}

	public void handleText(char[] data, int pos) {
		String strData = new String(data);
		if(messageFlag){
			message += strData;
			messageFlag = false;
		}else if(causeFlag) {
			message += " - " + strData;
			causeFlag = false;
		}else {
			if(strData.equalsIgnoreCase("description"))
				messageFlag = true;
			else if(strData.equalsIgnoreCase("caused by:"))
				causeFlag = true;
		}
	}

	public void handleStartTag(Tag t, MutableAttributeSet a, int p) {
	}

	public String getMessage() {
		return this.message;
	}
}
