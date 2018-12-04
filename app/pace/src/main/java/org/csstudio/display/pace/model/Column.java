/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pace.model;

import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Definition of a PACE model column.
 *
 *  <p>Describes one column in the table/model.
 *  The PV is described with macros,
 *  and the Instance definition then replaces the macros
 *  to get the actual PV for a Cell.
 *
 *  @author Delphy Nypaver Armstrong
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Column
{
    final String name;
    final String pv_with_macros;
    final boolean readonly;
    final String namePv_with_macros;
    final String datePv_with_macros;
    final String commentPv_with_macros;

    /** Parse and create Column from DOM
     *  @param col_node DOM node for column info
     *  @return Column
     *  @throws Exception on missing DOM elements
     */
    public static Column fromDOM(final Element col_node) throws Exception
    {
        // Get expected sub-elements
        final String name = XMLUtil.getChildString(col_node, "name").get();
        final String pv_pattern = XMLUtil.getChildString(col_node, "pv").get();
        final String access = XMLUtil.getChildString(col_node, "access").get();
        if (name.length() <= 0)
            throw new Exception("Missing column name");
        if (pv_pattern.length() <= 0)
            throw new Exception("Missing PV name pattern");
        // Look in the XML file to see if the column has comment, name and/or
        // date pvs.
        final String name_pv = XMLUtil.getChildString(col_node, "name_pv").orElse("");
        final String date_pv = XMLUtil.getChildString(col_node, "date_pv").orElse("");
        final String comment_pv = XMLUtil.getChildString(col_node, "comment_pv").orElse("");
        // When access=="ro", make read-only. Otherwise, including no access
        // info, use read/write
        return new Column(name, pv_pattern, "ro".equalsIgnoreCase(access),
              name_pv, date_pv, comment_pv);
    }

    /** Initialize Column
     *  @param name Column name/title
     *  @param pv_with_macros Name of the PV with macros
     *  @param readonly read-only column?
     */
    public Column(final String name, final String pv_with_macros,
            final boolean readonly, final String name_pv, final String date_pv,
            final String comment_pv)
    {
        this.name = name;
        this.pv_with_macros = pv_with_macros;
        this.readonly = readonly;
        this.namePv_with_macros = name_pv;
        this.datePv_with_macros = date_pv;
        this.commentPv_with_macros = comment_pv;
    }

    /** @return Column name/title */
    public String getName()
    {
        return name;
    }


    /** @return <code>true</code> for read-only column */
    public boolean isReadonly()
    {
        return readonly;
    }

    /** @return Name of the PV with unexpanded macros */
    protected String getPvWithMacros()
    {
        return pv_with_macros;
    }

    /** @return Column name, PV, access representation for debugging */
    @Override
    public String toString()
    {
        return "Column '" + name + "', PV '" + pv_with_macros + "'" +
               (readonly ? " (read-only)" : "");
    }

    /** @return macro string of the pv for the name of the person making the change or "" */
    public String getNamePvWithMacros()
    {
        return namePv_with_macros;
    }

    /** @return macro string of the pv for the comment made with the change or "" */
    public String getCommentPvWithMacros()
    {
        return commentPv_with_macros;
    }

    /** @return macro string of the pv for the date/time the change was made or "" */
    public String getDatePvWithMacros()
    {
        return datePv_with_macros;
    }
}
