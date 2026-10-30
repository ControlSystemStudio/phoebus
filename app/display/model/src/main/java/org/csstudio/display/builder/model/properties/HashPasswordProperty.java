package org.csstudio.display.builder.model.properties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;

@SuppressWarnings("nls")
public class HashPasswordProperty extends WidgetProperty<String>
{
    public HashPasswordProperty(
            final WidgetPropertyDescriptor<String> descriptor,
            final Widget widget,
            final String default_value)
    {
        super(descriptor, widget, default_value);
    }

    @Override
    public void setValueFromObject(final Object value)
    {
        if (value == null)
            setValue("");
        else
            setValue(value.toString());
    }

    @Override
    public void writeToXML(final ModelWriter model_writer,
                           final XMLStreamWriter writer) throws Exception
    {
        writer.writeCharacters(value);
    }

    @Override
    public void readFromXML(final ModelReader model_reader,
                            final Element property_xml) throws Exception
    {
        setValue(XMLUtil.getString(property_xml));
    }

    public static String crypt(final String password)
    {
        if (password == null || password.isEmpty())
            return password;

        try
        {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] hash =
                    md.digest(password.getBytes(StandardCharsets.UTF_8));

            final StringBuilder hex = new StringBuilder(hash.length * 2);

            for (final byte b : hash)
                hex.append(String.format("%02x", b));

            return hex.toString();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to hash password", e);
        }
    }
}
