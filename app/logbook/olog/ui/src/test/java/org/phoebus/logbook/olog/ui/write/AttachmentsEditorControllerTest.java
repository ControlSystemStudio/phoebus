/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.logbook.olog.ui.write;

import org.junit.jupiter.api.Test;
import org.phoebus.olog.es.api.model.OlogLog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AttachmentsEditorControllerTest {

    @Test
    public void testRemoveImageMarkup(){
        AttachmentsEditorController attachmentsEditorController =
                new AttachmentsEditorController(new OlogLog());

        String markup = "![](attachment/123456789){width=100 height=100}";
        String result = attachmentsEditorController.removeImageMarkup(markup, "123456789");
        assertTrue(result.isEmpty());

        markup = "ABC ![](attachment/123456789){width=100 height=100} DEF";
        result = attachmentsEditorController.removeImageMarkup(markup, "123456789");
        assertEquals("ABC  DEF", result);

        markup = "![](attachment/ABCDE){width=100 height=100}\n![](attachment/123456789){width=100 height=100}\n![](attachment/abcde){width=100 height=100}";
        result = attachmentsEditorController.removeImageMarkup(markup, "123456789");
        assertEquals("![](attachment/ABCDE){width=100 height=100}\n\n![](attachment/abcde){width=100 height=100}", result);

        markup = "![](attachment/123456789){width=100 height=100}";
        result = attachmentsEditorController.removeImageMarkup(markup, "abcde");
        assertEquals("![](attachment/123456789){width=100 height=100}", result);

        markup = "whatever";
        result = attachmentsEditorController.removeImageMarkup(markup, "123456789");
        assertEquals("whatever", result);
    }
}
