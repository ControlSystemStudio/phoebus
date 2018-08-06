/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

import org.csstudio.archive.writer.rdb.TimestampHelper;

/** Helper for creating uniform HTML pages for a servlet response.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class HTMLWriter
{
    final protected static String BACKGROUND = "images/blueback.jpg"; //$NON-NLS-1$
    final protected static String RED_FONT = "<font color='#ff0000'>"; //$NON-NLS-1$
    final protected static String CLOSE_FONT = "</font>"; //$NON-NLS-1$

    /** Writer */
    final private PrintWriter html;

    /** Helper for marking every other table line */
    private boolean odd_table_line = true;

    /** @return HTML Writer with start of HTML page.
     *  @param resp Response for which to create the writer
     *  @param title HTML title
     *  @throws IOException On error
     */
    public HTMLWriter(final HttpServletResponse resp, final String title) throws IOException
    {
        resp.setContentType("text/html");
        html = resp.getWriter();
        text("<html>");
        text("<head>");
        text("<title>" + title + "</title>");
        text("<script type=\"text/javascript\" src=\"/sorttable.js\"></script>\n");
        text("</head>");
        text("<body background='" + BACKGROUND + "'>");
        text("<blockquote>");
        h1(title);
    }

    /** Add end of HTML page. */
    public void close()
    {
        text("<p>");
        text("<hr width='50%' align='left'>");

        text("<a href=\"/main\">-Main-</a> ");
        text("<a href=\"/groups\">-Groups-</a> ");
        text("<a href=\"/disconnected\">-Disconnected-</a> ");
        text("<a href=\"/version.html\">-Version-</a> ");

        text("<address>");
        text(TimestampHelper.format(Instant.now()));
        text("   <i>(Use web browser's Reload to refresh this page)</i>");
        text("</address>");

        text("</blockquote>");
        text("</body>");
        text("</html>");
        html.close();
    }

    /** Add text to HTML */
    protected void text(final String text)
    {
        html.println(text);
    }
    /** Add header */
    protected void h1(final String text)
    {
        text("<h1>" + text + "</h1>");
    }

    /** Add header */
    protected void h2(final String text)
    {
        text("<h2>" + text + "</h2>");
    }

    /** Start a table.
     *  <p>
     *  The intial column header might span more than one column.
     *  In fact, it might be the only columns header.
     *  Otherwise, the remaining column headers each span one column.
     *
     *  @param initial_colspan Number of columns for the first header.
     *  @param header Headers for all the columns
     *  @see #tableLine(String[])
     *  @see #closeTable()
     */
    protected void openTable(final int initial_colspan, final String... headers)
    {
        text("<table border='0' class='sortable'>");
        text("<thead>");
        text("  <tr bgcolor='#AAAAFF'>");
        text("    <th align='center' colspan='" + initial_colspan + "'>" +
                        headers[0] + "</th>");
        for (int i=1; i<headers.length; ++i)
            text("    <th align='center'>" + headers[i] + "</th>");
        text("  </tr>");
        text("</thead>");
        text("<tbody>");
        odd_table_line = true;
    }

    /** One table line.
     *  @param columns Text for each column.
     *                 Count must match the colspan of openTable
     *  @see #openTable(int, String[])
     */
    protected void tableLine(final String... columns)
    {
        text("  <tr>");
        boolean first = true;
        for (String column : columns)
        {
            if (first)
            {
                first = false;
                if (odd_table_line)
                    text("    <th align='left' valign='top'>" + column + "</th>");
                else
                    text("    <th align='left' valign='top' bgcolor='#DFDFFF'>" + column + "</th>");
            }
            else
            {
                if (odd_table_line)
                    text("    <td align='center' valign='top'>" + column + "</td>");
                else
                    text("    <td align='center' valign='top' bgcolor='#DFDFFF'>" + column + "</td>");
            }
        }
        text("  </tr>");
        odd_table_line = !odd_table_line;
    }

    /** Close a table */
    protected void closeTable()
    {
        text("</tbody>");
        text("</table>");
    }

    /** Create HTML for a link
     *  @param url Linked URL
     *  @param text Text to display
     *  @return HTML for the link
     */
    public static String makeLink(final String url, final String text)
    {
        return "<a href=\"" + url + "\">" + text + "</a>";
    }

    /** @return HTML for red text */
    public static String makeRedText(final String text)
    {
        return RED_FONT + text + CLOSE_FONT;
    }

}
