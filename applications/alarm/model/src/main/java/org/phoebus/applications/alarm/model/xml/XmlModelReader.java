package org.phoebus.applications.alarm.model.xml;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XmlModelReader
{
	public static final String TAG_CONFIG = "config";

	// Parse the xml stream and load the stream into a document.
	public void load(InputStream stream) throws Exception
	{
        final DocumentBuilder docBuilder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document doc = docBuilder.parse(stream);

        buildModel(doc);
	}

	private void buildModel(final Document doc) throws Exception
	{
		// Check if it's a <config/>.
        doc.getDocumentElement().normalize();
        final Element root_node = doc.getDocumentElement();
        if (!root_node.getNodeName().equals(TAG_CONFIG))
            throw new Exception("Expected " + TAG_CONFIG + " but got " + root_node.getNodeName());
        else
        	System.out.println("config");
	}
}
