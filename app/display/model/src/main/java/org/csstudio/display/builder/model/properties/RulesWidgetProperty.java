/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.rules.RuleInfo;
import org.csstudio.display.builder.model.rules.RuleInfo.ExpressionInfo;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget property that describes rules
 *
 *  @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class RulesWidgetProperty extends WidgetProperty<List<RuleInfo>>
{
    /** Pattern for "pvSev0", "pvSev1", .. */
    private static final Pattern PVSEV_PATTERN = Pattern.compile("pvSev([0-9]+)");

    private static final WidgetPropertyDescriptor<String> miscUnknownPropID =
            new WidgetPropertyDescriptor<String>(WidgetPropertyCategory.MISC,
                    "rule_unknown_propid", "RulesWidgetProperty:miscUnknownPropID", false)
    {
        @Override
        public WidgetProperty<String> createProperty(final Widget widget, final String value)
        {
            return new StringWidgetProperty(this, widget, value);
        }
    };


    public static WidgetProperty<?> propIDToNewProp(final Widget widget,
            final String prop_id, final String dbg_tag)
    {
        try
        {
            return widget.getPropertyByPath(prop_id, true).clone();
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, widget + " cannot make new unknown property id " + prop_id, ex);

            if ((dbg_tag != null) && (dbg_tag.length() > 0))
                return miscUnknownPropID.createProperty(null, dbg_tag);
            else
                return miscUnknownPropID.createProperty(null, prop_id + "?");
        }
    }

    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public RulesWidgetProperty(
            final WidgetPropertyDescriptor<List<RuleInfo>> descriptor,
            final Widget widget,
            final List<RuleInfo> default_value)
    {
        super(descriptor, widget, default_value);
    }

    /** @param value Must be RuleInfo array or List */
    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        if (value instanceof RuleInfo[])
            setValue(Arrays.asList((RuleInfo[]) value));
        else if (value instanceof Collection)
        {
            final List<RuleInfo> rules = new ArrayList<>();
            for (Object item : (Collection<?>)value)
                if (item instanceof RuleInfo)
                    rules.add((RuleInfo) item);
                else
                    throw new Exception("Need RuleInfo[], got " + value);
            setValue(rules);
        }
        else
            throw new Exception("Need RuleInfo[], got " + value);
    }

    @Override
    public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception
    {
        logger.log(Level.FINE, "Write " + value.size() + " rules to XML");
        for (final RuleInfo info : value)
        {
            // <rule name="name" prop_id="prop" out_exp="true">
            writer.writeStartElement(XMLTags.RULE);
            writer.writeAttribute(XMLTags.NAME, info.getName());
            writer.writeAttribute("prop_id", info.getPropID());
            writer.writeAttribute("out_exp", String.valueOf(info.getPropAsExprFlag()));

            for ( final ExpressionInfo<?> expr : info.getExpressions())
            {
                // <exp bool_exp="foo==1">
                writer.writeStartElement("exp");
                writer.writeAttribute("bool_exp", expr.getExp());

                if (info.getPropAsExprFlag())
                {
                    // <expression>
                    writer.writeStartElement("expression");
                    if (!(expr.getPropVal() instanceof String))
                    {
                        logger.log(Level.SEVERE, "Mismatch of rules output expression flag with expression value type, expected String, got ", expr.getPropVal().getClass());
                        writer.writeCharacters("ERROR");
                    }
                    // some string of the value or expression
                    writer.writeCharacters((String)expr.getPropVal());
                }
                else
                {
                    // <value>
                    writer.writeStartElement("value");
                    if (!(expr.getPropVal() instanceof WidgetProperty<?>))
                    {
                        logger.log(Level.SEVERE, "Mismatch of rules output expression flag with expression value type, expected Widget Property, got ", expr.getPropVal().getClass());
                        writer.writeCharacters("ERROR");
                    }
                    // write the property
                    ((WidgetProperty<?>)expr.getPropVal()).writeToXML(model_writer, writer);
                }
                // </value> or </expression>
                writer.writeEndElement();
                // </exp>
                writer.writeEndElement();
            }
            //</rule>
            writer.writeEndElement();
        }
    }

    @Override
    public void readFromXML(final ModelReader model_reader, final Element property_xml) throws Exception
    {
        final List<RuleInfo> rules = new ArrayList<>();
        for (final Element xml : XMLUtil.getChildElements(property_xml, XMLTags.RULE))
        {
            String name, prop_id, out_exp_str;

            try
            {
                name = xml.getAttribute(XMLTags.NAME);
                if (name.isEmpty())
                    logger.log(Level.WARNING, "Missing rule 'name'");
            }
            catch (Exception ex)
            {
                name = "unknown";
                logger.log(Level.WARNING, "Failed to find rule name");
            }


            try
            {
                prop_id = xml.getAttribute("prop_id");
                if (prop_id.isEmpty())
                    logger.log(Level.WARNING, "Missing rule 'prop_id'");
            }
            catch (Exception e)
            {
                prop_id = "unknown";
                logger.log(Level.WARNING, "Failed to find rule prop_id");
            }


            boolean prop_as_expr = false;
            try
            {
                out_exp_str = xml.getAttribute("out_exp");
                prop_as_expr = false;
                if (out_exp_str.isEmpty())
                    logger.log(Level.WARNING, "Missing rule 'out_exp'");
                else
                    prop_as_expr = Boolean.parseBoolean(out_exp_str);
            }
            catch (Exception e)
            {
                logger.log(Level.WARNING, "Failed to find rule out_exp");
            }

            List<ExpressionInfo<?>> exprs;
            try
            {
                exprs = readExpressions(model_reader, prop_id, prop_as_expr, xml);
            }
            catch (Throwable ex)
            {
                logger.log(Level.WARNING, "Failure to readExpressions for " + prop_id, ex);
                exprs = Collections.emptyList();
            }

            final List<ScriptPV> pvs = readPVs(xml);
            rules.add(new RuleInfo(name, prop_id, prop_as_expr, exprs));
        }
        setValue(rules);
    }

    private List<ExpressionInfo<?>> readExpressions(final ModelReader model_reader,
            final String prop_id,
            final boolean prop_as_expr,
            final Element xml) throws Exception
    {
        // Replace "pvSev0" by "pvLegacySev0"?
        final boolean patch_severity = model_reader.getVersion().getMajor() < 2;

        final List<ExpressionInfo<?>> exprs = new ArrayList<>();
        final String tagstr = (prop_as_expr) ? "expression" : "value";
        for (final Element exp_xml : XMLUtil.getChildElements(xml, "exp"))
        {
            String bool_exp = exp_xml.getAttribute("bool_exp");
            if (bool_exp.isEmpty())
                logger.log(Level.WARNING, "Missing exp 'bool_exp'");

            if (patch_severity  &&  bool_exp.contains("pvSev"))
            {
                logger.log(Level.WARNING, "Patching rule with legacy '" + bool_exp + "' into 'pvLegacySev..'");
                bool_exp = PVSEV_PATTERN.matcher(bool_exp).replaceAll("pvLegacySev$1");
            }

            final Element tag_xml = XMLUtil.getChildElement(exp_xml, tagstr);
            // Legacy case where value is used for all value expression
            final Element val_xml = (tag_xml == null) ? XMLUtil.getChildElement(exp_xml, "value") : tag_xml;

            if (prop_as_expr)
            {
                final String val_str = (val_xml != null) ? XMLUtil.getString(val_xml) : "";
                exprs.add(new ExpressionInfo(bool_exp, false, val_str));
            }
            else
            {
                final String val_str = (val_xml != null) ? XMLUtil.elementsToString(val_xml.getChildNodes(), false) : "";

                final WidgetProperty<?> val_prop = propIDToNewProp(getWidget(), prop_id, val_str);

                if ( ! miscUnknownPropID.getName().equals(val_prop.getName()))
                    val_prop.readFromXML(model_reader, val_xml);
                exprs.add(new ExpressionInfo<>(bool_exp, true, val_prop));
            }
        }
        return exprs;
    }

    private List<ScriptPV> readPVs(final Element xml)
    {
        final List<ScriptPV> pvs = new ArrayList<>();
        // Legacy used just 'pv'
        final Iterable<Element> pvs_xml;
        if (XMLUtil.getChildElement(xml, XMLTags.PV_NAME) != null)
            pvs_xml = XMLUtil.getChildElements(xml, XMLTags.PV_NAME);
        else
            pvs_xml = XMLUtil.getChildElements(xml, "pv");
        for (final Element pv_xml : pvs_xml)
        {   // Unless either the new or old attribute is _present_ and set to false,
            // default to triggering on this PV
            final boolean trigger =
                    XMLUtil.parseBoolean(pv_xml.getAttribute(XMLTags.TRIGGER), true) &&
                    XMLUtil.parseBoolean(pv_xml.getAttribute("trig"), true);
            final String name = XMLUtil.getString(pv_xml);
            pvs.add(new ScriptPV(name, trigger));
        }
        return pvs;
    }
}
