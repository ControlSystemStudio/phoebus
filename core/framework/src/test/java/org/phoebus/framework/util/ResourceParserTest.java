package org.phoebus.framework.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.phoebus.framework.util.ResourceParser.PV_SCHEMA;
import static org.phoebus.framework.util.ResourceParser.createResourceURI;
import static org.phoebus.framework.util.ResourceParser.getAppName;
import static org.phoebus.framework.util.ResourceParser.getContent;
import static org.phoebus.framework.util.ResourceParser.getFile;
import static org.phoebus.framework.util.ResourceParser.getURI;
import static org.phoebus.framework.util.ResourceParser.parsePVs;
import static org.phoebus.framework.util.ResourceParser.parseQueryArgs;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

/**
 * JUnit test of {@link ResourceParser}
 *
 * @author Kunal Shroff
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ResourceParserTest
{
    @Test
    public void checkFileToURI() throws Exception
    {
        // File URL
        URI uri = createResourceURI("file:/some/file/path");
        System.out.println(uri);
        assertThat(uri.getScheme(), equalTo("file"));
        assertThat(uri.getPath(), equalTo("/some/file/path"));

        // File URL with application hint
        uri = createResourceURI("file:/some/file/path?app=probe");
        System.out.println(uri);
        assertThat(uri.getScheme(), equalTo("file"));
        // File stays the same, the app hint in the query doesn't change that
        assertThat(uri.getPath(), equalTo("/some/file/path"));

        // A test resource relative to where this test is running
        final String relative = "src/test/resources/test.plt";

        // Absolute file path
        final File file = new File(".", relative).getCanonicalFile();
        final String absolute = file.getCanonicalPath();

        // Test if absolute path is turned into URI
        uri = createResourceURI(absolute);
        System.out.println(uri);
        assertThat(uri.getScheme(), equalTo("file"));
        assertThat(uri.getPath(), equalTo(absolute));

        // Turn relative file path into URI, which should then be absolute
        uri = createResourceURI(relative);
        System.out.println(uri);
        assertThat(uri.getScheme(), equalTo("file"));
        assertThat(uri.getPath(), equalTo(absolute));

        // Convert between File and URI
        assertThat(getFile(uri), equalTo(file));
        assertThat(getURI(file), equalTo(uri));

        // Read content
        final InputStream stream = getContent(uri);
        assertThat(stream, not(nullValue()));
        stream.close();

        // Nonexisting file
        uri = createResourceURI("some/bogus/file");
        assertThat(uri, not(nullValue()));
        final File bogus = getFile(uri);
        System.out.println(bogus);
        assertThat(bogus.exists(), equalTo(false));

        try
        {
            getContent(uri);
            fail("Read nonexisting file?");
        }
        catch (Exception ex)
        {
            // Good, caught it
        }
    }

    @Test
    public void checkWebToURI() throws Exception
    {
        // Web URL
        URI uri = createResourceURI("http://some.site/file/path");
        System.out.println(uri);
        assertThat(uri.getScheme(), equalTo("http"));
        assertThat(uri.getHost(), equalTo("some.site"));
        assertThat(uri.getPath(), equalTo("/file/path"));
    }

    @Test
    public void checkPVs() throws Exception
    {
        // PV URL with one PV
        URI uri = createResourceURI("pv://?Fred");
        System.out.println(uri);
        assertThat(uri.getScheme(), equalTo(PV_SCHEMA));

        List<String> pvs = parsePVs(uri);
        System.out.println(pvs);
        assertThat(pvs.size(), equalTo(1));
        assertThat(pvs, hasItems("Fred"));

        // Several PVs (but don't include the application hint)
        uri = createResourceURI("pv://?Fred&Jane&AnotherPV&app=probe");
        System.out.println(uri);
        assertThat(uri.getScheme(), equalTo(PV_SCHEMA));

        pvs = parsePVs(uri);
        System.out.println(pvs);
        assertThat(pvs.size(), equalTo(3));
        assertThat(pvs, hasItems("Fred", "Jane" , "AnotherPV"));

        // Not a PV URL -> empty result
        pvs = parsePVs(createResourceURI("http://some.site"));
        assertThat(pvs.size(), equalTo(0));

        // Check errors
        try
        {   // "pvs://" without actual PVs
            parsePVs(createResourceURI("pv://some.site"));
            fail("Failed to detect missing PVs");
        }
        catch (Exception ex)
        {
            // Good, got exception
            System.out.println(ex.getMessage());
        }
    }

    @Test
    public void checkApplicationHint() throws Exception
    {
        // Plain URL, no application hint
        URI uri = createResourceURI("pv://?Fred");
        Optional<String> app = getAppName(uri);
        assertThat(app.isPresent(), equalTo(false));

        // PVs with application hint
        uri = createResourceURI("pv://?Fred&app=probe");
        app = getAppName(uri);
        assertThat(app.isPresent(), equalTo(true));
        assertThat(app.get(), equalTo("probe"));

        // File URL with application hint
        uri = createResourceURI("file:/path/to/file?app=probe");
        app = getAppName(uri);
        assertThat(app.isPresent(), equalTo(true));
        assertThat(app.get(), equalTo("probe"));
    }

    @Test
    public void checkQueryItems() throws Exception
    {
        // URI with a bunch of query itens
        URI uri = createResourceURI("file://some/file?Fred&X=1&Y=2&app=probe&Z=47");

        final Map<String, List<String>> items = parseQueryArgs(uri);
        System.out.println(items);

        assertThat(items.keySet(), hasItems("Fred", "X", "Y", "Z"));
        assertThat(items.keySet(), not(hasItems("app")));

        List<String> values = items.get("Fred");
        assertThat(values.size(), equalTo(1));
        assertThat(values.get(0), nullValue());

        values = items.get("Y");
        assertThat(values.size(), equalTo(1));
        assertThat(values.get(0), equalTo("2"));
    }
}
