/**
 * 
 */
package org.phoebus.olog.api;


import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import javax.swing.text.html.parser.ParserDelegator;

/**
 *
 * @author berryman from shroffk
 *
 */
public class OlogException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6279865221993808192L;

	private int status;
	
	public OlogException() {
		super();
	}

	public OlogException(String message){
		super(message);
	}

	public OlogException(int status, String message){
		super(message);
		this.status = status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}


}
