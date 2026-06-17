package org.phoebus.framework.util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.phoebus.framework.util.ResourceParser.PV_SCHEMA;
import static org.phoebus.framework.util.ResourceParser.createResourceURI;
import static org.phoebus.framework.util.ResourceParser.getAppName;
import static org.phoebus.framework.util.ResourceParser.getContent;
import static org.phoebus.framework.util.ResourceParser.getFile;
import static org.phoebus.framework.util.ResourceParser.getTargetName;
import static org.phoebus.framework.util.ResourceParser.getURI;
import static org.phoebus.framework.util.ResourceParser.parsePVs;
import static org.phoebus.framework.util.ResourceParser.parseQueryArgs;

/**
 * JUnit test of {@link ResourceParser}
 *
 * @author Kunal Shroff
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ResourceParserTest
{

    private static final String OS = System.getProperty("os.name").toLowerCase();

    @Test
    public void checkFileToURI() throws Exception {
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
        assertThat(Paths.get(uri).toString(), equalTo(absolute));

        // Turn relative file path into URI, which should then be absolute
        uri = createResourceURI(relative);
        System.out.println(uri);
        assertThat(uri.getScheme(), equalTo("file"));
        assertThat(Paths.get(uri).toString(), equalTo(absolute));

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

        try {
            getContent(uri);
            fail("Read nonexisting file?");
        } catch (Exception ex) {
            // Good, caught it
        }

        // File with space
        final File spacey = new File("/some/dir with space/file.abc");
        uri = createResourceURI(spacey.getPath());
        assertThat(uri, not(nullValue()));
        assertThat(new File(uri).getCanonicalFile(), equalTo(spacey.getCanonicalFile()));
        assertThat(uri.getScheme(), equalTo("file"));
        if (OS.indexOf("win") >= 0) {
            assertTrue(uri.toString().matches("file:/[a-zA-Z]:/some/dir%20with%20space/file.abc"));
        } else {
            assertThat(uri.toString(), equalTo("file:/some/dir%20with%20space/file.abc"));
        }

        // UNC-style file URI must keep host when converted to File
        final URI unc = createResourceURI("file://wsl.localhost/AlmaLinux-9/home/jwlodek/test.bob");
        final File unc_file = getFile(unc);
        assertThat(unc_file, not(nullValue()));
        assertTrue(unc_file.getPath().toLowerCase().contains("wsl.localhost"));
        assertTrue(unc_file.getPath().toLowerCase().contains("almalinux-9"));

        // Query parameters do not change underlying file path
        final URI unc_with_query = createResourceURI("file://wsl.localhost/AlmaLinux-9/home/jwlodek/test.bob?app=display_editor");
        final File queried = getFile(unc_with_query);
        assertThat(queried, not(nullValue()));
        assertThat(queried.getPath(), equalTo(unc_file.getPath()));
    }

    @Test
    public void checkUNCPaths() throws Exception
    {
        // UNC file URI with host -> getFile preserves host in path
        final URI unc_uri = new URI("file", "wsl.localhost", "/AlmaLinux-9/home/user/display.bob", null);
        assertThat(unc_uri.getHost(), equalTo("wsl.localhost"));
        assertThat(unc_uri.getPath(), equalTo("/AlmaLinux-9/home/user/display.bob"));

        final File unc_file = getFile(unc_uri);
        assertThat(unc_file, not(nullValue()));
        // File path must contain both the host and the share path
        final String unc_path = unc_file.getPath();
        assertTrue(unc_path.contains("wsl.localhost"), "UNC file path should contain host: " + unc_path);
        assertTrue(unc_path.contains("AlmaLinux-9"), "UNC file path should contain share: " + unc_path);

        // getURI for a UNC File should produce a URI with proper host
        final File unc_input = new File("//wsl.localhost/AlmaLinux-9/home/user/display.bob");
        final URI round_trip = getURI(unc_input);
        assertThat(round_trip.getScheme(), equalTo("file"));
        // On Linux, File("//host/...").toURI() may give file:///host/...;
        // on Windows, it gives file:////host/.... Either way our getURI()
        // should normalize it so the host is accessible:
        if (round_trip.getHost() != null)
        {
            assertThat(round_trip.getHost(), equalTo("wsl.localhost"));
            assertThat(round_trip.getPath(), equalTo("/AlmaLinux-9/home/user/display.bob"));
        }
        else
        {
            // On some platforms, the host ends up in the path as //host/path
            assertTrue(round_trip.getPath().startsWith("//wsl.localhost/")
                    || round_trip.getPath().startsWith("/wsl.localhost/"),
                    "Path should contain host: " + round_trip.getPath());
        }

        // Round-trip: getFile(getURI(file)) should preserve the host
        final File round_trip_file = getFile(round_trip);
        assertThat(round_trip_file, not(nullValue()));
        assertTrue(round_trip_file.getPath().contains("wsl.localhost"),
                "Round-trip file path should contain host: " + round_trip_file.getPath());

        // UNC URI with query parameters should not lose host
        final URI unc_query = new URI("file", "server.example", "/share/path/file.bob", "app=display_runtime");
        final File unc_query_file = getFile(unc_query);
        assertThat(unc_query_file, not(nullValue()));
        assertTrue(unc_query_file.getPath().contains("server.example"),
                "Query file path should contain host: " + unc_query_file.getPath());

        // UNC URI with no host (path starts with //) should preserve the double slash
        final URI unc_no_host = URI.create("file:////server/share/path/file.bob");
        final File unc_no_host_file = getFile(unc_no_host);
        assertThat(unc_no_host_file, not(nullValue()));
        assertTrue(unc_no_host_file.getPath().contains("server"),
                "No-host UNC file path should contain server: " + unc_no_host_file.getPath());
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
        uri = createResourceURI("pv://?Fred&Jane&AnotherPV&app=probe&target=test");
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
        String app = getAppName(uri);
        assertThat(app, nullValue());

        // PVs with application hint
        uri = createResourceURI("pv://?Fred&app=probe");
        app = getAppName(uri);
        assertThat(app, equalTo("probe"));

        // File URL with application hint
        uri = createResourceURI("file:/path/to/file?app=probe");
        app = getAppName(uri);
        assertThat(app, equalTo("probe"));
    }

    @Test
    public void checkTargetHint() throws Exception
    {
        // Plain URL, no pane hint
        URI uri = createResourceURI("pv://?Fred");
        String target = getTargetName(uri);
        assertThat(target, nullValue());

        // PVs with pane hint
        uri = createResourceURI("pv://?Fred&target=test");
        target = getTargetName(uri);
        assertThat(target, equalTo("test"));

        // File URL with pane hint
        uri = createResourceURI("file:/path/to/file?target=test");
        target = getTargetName(uri);
        assertThat(target, equalTo("test"));
    }

    @Test
    public void checkQueryItems() throws Exception
    {
        // URI with a bunch of query itens
        URI uri = createResourceURI("file://some/file?Fred&X=1&Y=2+3&app=probe&Z=2%2B3&target=test");

        final Map<String, List<String>> items = parseQueryArgs(uri);
        System.out.println(items);

        assertThat(items.keySet(), hasItems("Fred", "X", "Y", "Z"));
        assertThat(items.keySet(), not(hasItems("app")));
        assertThat(items.keySet(), not(hasItems("test")));

        List<String> values = items.get("Fred");
        assertThat(values.size(), equalTo(1));
        assertThat(values.get(0), equalTo(""));

        values = items.get("Y");
        assertThat(values.size(), equalTo(1));
        assertThat(values.get(0), equalTo("2 3"));

        values = items.get("Z");
        assertThat(values.size(), equalTo(1));
        assertThat(values.get(0), equalTo("2+3"));
    }
}
