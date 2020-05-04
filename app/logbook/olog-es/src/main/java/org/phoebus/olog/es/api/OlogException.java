/**
 * 
 */
package org.phoebus.olog.es.api;


import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.UniformInterfaceException;
import java.io.IOException;
import java.io.FileNotFoundException;
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

	public OlogException() {
		super();
	}

	public OlogException(String message) {
		super(message);
	}
}
