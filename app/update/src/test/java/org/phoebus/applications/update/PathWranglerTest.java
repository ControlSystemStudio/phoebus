/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.update;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/** JUnit test of {@link PathWrangler}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PathWranglerTest
{
    @Test
    public void testPathWrangler()
    {
        final PathWrangler wrangler = new PathWrangler("phoebus\\.app/Content/.*,phoebus\\.app/,phoebus-[^/]+/,product-[^/]+/,jdk/.*");

        // Strip product subdir
        assertThat(wrangler.wrangle("phoebus-0.0.1/doc/applications.html"), equalTo("doc/applications.html"));
        assertThat(wrangler.wrangle("product-sns-0.0.1/lib/somelib.jar"), equalTo("lib/somelib.jar"));
        assertThat(wrangler.wrangle("product-sns-0.0.1/doc/phoebus/app/display/editor/doc/html/generated/org/csstudio/display/builder/model/DisplayModel.html"),
                            equalTo("doc/phoebus/app/display/editor/doc/html/generated/org/csstudio/display/builder/model/DisplayModel.html"));

        // Skip JDK
        assertThat(wrangler.wrangle("jdk/bin/java"), equalTo(""));

        // Mac OS App
        assertThat(wrangler.wrangle("phoebus.app/product-sns-0.0.1/lib/somelib.jar"), equalTo("lib/somelib.jar"));
        assertThat(wrangler.wrangle("phoebus.app/jdk/bin/java"), equalTo(""));
        assertThat(wrangler.wrangle("phoebus.app/Content/Whateer/Else"), equalTo(""));
    }
}
