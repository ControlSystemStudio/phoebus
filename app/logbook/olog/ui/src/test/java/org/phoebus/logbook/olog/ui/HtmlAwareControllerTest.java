/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.logbook.olog.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HtmlAwareControllerTest {

    @Test
    public void testEscapeHtml(){
        HtmlAwareController htmlAwareController = new HtmlAwareController("");
        String escapedHtml = htmlAwareController.toHtml("<br><p>Paragraph</p>");
        assertEquals("<p>&lt;br&gt;&lt;p&gt;Paragraph&lt;/p&gt;</p>\n", escapedHtml);
    }
}
