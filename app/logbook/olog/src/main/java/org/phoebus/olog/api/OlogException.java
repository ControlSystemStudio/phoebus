/**
 * 
 */
package org.phoebus.olog.api;


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
	
	private Status status;
	
	public OlogException() {
		super();
	}

	public OlogException(String message){
		super(message);
	}

	public OlogException(UniformInterfaceException cause) {
		super(parseErrorMsg(cause), cause);
		this.setStatus(Status.fromStatusCode(cause.getResponse().getStatus()));
	}

	private static String parseErrorMsg(UniformInterfaceException ex) {
            String entity = ex.getResponse().getEntity(String.class);
            try {
                    ClientResponseParser callback = new ClientResponseParser();
                    Reader reader = new StringReader(entity);
                    new ParserDelegator().parse(reader, callback, false);
                    return callback.getMessage();
            } catch (IOException e) {
                return "Could not retrieve message from server";
            }
	}


	/**
	 * @param status the status to set
	 */
	public void setStatus(Status status) {
		this.status = status;
	}

	/**
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}


}
