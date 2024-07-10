/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import org.junit.jupiter.api.Test;
import org.phoebus.applications.alarm.ui.AlarmURI;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/** {@link AlarmURI} Tests
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmURITest {

    @Test
	public void createURI() {
        URI uri = AlarmURI.createURI("localhost:9092", "Accelerator");
		assertThat(uri.toString(), equalTo("alarm://localhost:9092/Accelerator"));

        uri = AlarmURI.createURI("localhost:9092", "Accelerator", "param=value");
        assertThat(uri.toString(), equalTo("alarm://localhost:9092/Accelerator?param=value"));
	}

    @Test
    public void parseAlarmURI() throws Exception {
        // create with URI.create
        // with / without default port
        String[] parsed = AlarmURI.parseAlarmURI(URI.create("alarm://localhost:9092/Accelerator"));
        assertThat(parsed.length, equalTo(3));
        assertThat(parsed[0], equalTo("localhost:9092"));
        assertThat(parsed[1], equalTo("Accelerator"));
        assertThat(parsed[2], equalTo(null));

        parsed = AlarmURI.parseAlarmURI(URI.create("alarm://host.my.site/Test"));
        assertThat(parsed.length, equalTo(3));
        assertThat(parsed[0], equalTo("host.my.site:9092"));
        assertThat(parsed[1], equalTo("Test"));
        assertThat(parsed[2], equalTo(null));

        parsed = AlarmURI.parseAlarmURI(URI.create("alarm://host.my.site/Test?param"));
        assertThat(parsed.length, equalTo(3));
        assertThat(parsed[0], equalTo("host.my.site:9092"));
        assertThat(parsed[1], equalTo("Test"));
        assertThat(parsed[2], equalTo("param"));

        parsed = AlarmURI.parseAlarmURI(URI.create("alarm://host.my.site/Test?param=value"));
        assertThat(parsed.length, equalTo(3));
        assertThat(parsed[0], equalTo("host.my.site:9092"));
        assertThat(parsed[1], equalTo("Test"));
        assertThat(parsed[2], equalTo("param=value"));

        parsed = AlarmURI.parseAlarmURI(URI.create("alarm://host.my.site/Test?param=" + URLEncoder.encode("abc def", StandardCharsets.UTF_8)));
        assertThat(parsed.length, equalTo(3));
        assertThat(parsed[0], equalTo("host.my.site:9092"));
        assertThat(parsed[1], equalTo("Test"));
        assertThat(parsed[2], equalTo("param=" + URLEncoder.encode("abc def")));

        parsed = AlarmURI.parseAlarmURI(URI.create("alarm://host.my.site/Test?param=" + URLEncoder.encode("abc def", StandardCharsets.UTF_8) + "&param2=value"));
        assertThat(parsed.length, equalTo(3));
        assertThat(parsed[0], equalTo("host.my.site:9092"));
        assertThat(parsed[1], equalTo("Test"));
        assertThat(parsed[2], equalTo("param=" + URLEncoder.encode("abc def", StandardCharsets.UTF_8) + "&param2=value"));

        // create with AlarmURI.createURI
        // with / without default port
        parsed = AlarmURI.parseAlarmURI(AlarmURI.createURI("localhost:9092", "Accelerator"));
        assertThat(parsed.length, equalTo(3));
        assertThat(parsed[0], equalTo("localhost:9092"));
        assertThat(parsed[1], equalTo("Accelerator"));
        assertThat(parsed[2], equalTo(null));

        parsed = AlarmURI.parseAlarmURI(AlarmURI.createURI("host.my.site", "Test"));
        assertThat(parsed.length, equalTo(3));
        assertThat(parsed[0], equalTo("host.my.site:9092"));
        assertThat(parsed[1], equalTo("Test"));
        assertThat(parsed[2], equalTo(null));

        parsed = AlarmURI.parseAlarmURI(AlarmURI.createURI("host.my.site", "Test", "param"));
        assertThat(parsed.length, equalTo(3));
        assertThat(parsed[0], equalTo("host.my.site:9092"));
        assertThat(parsed[1], equalTo("Test"));
        assertThat(parsed[2], equalTo("param"));

        parsed = AlarmURI.parseAlarmURI(AlarmURI.createURI("host.my.site", "Test", "param=value"));
        assertThat(parsed.length, equalTo(3));
        assertThat(parsed[0], equalTo("host.my.site:9092"));
        assertThat(parsed[1], equalTo("Test"));
        assertThat(parsed[2], equalTo("param=value"));

        parsed = AlarmURI.parseAlarmURI(AlarmURI.createURI("host.my.site", "Test", "param=" + URLEncoder.encode("abc def", StandardCharsets.UTF_8)));
        assertThat(parsed.length, equalTo(3));
        assertThat(parsed[0], equalTo("host.my.site:9092"));
        assertThat(parsed[1], equalTo("Test"));
        assertThat(parsed[2], equalTo("param=" + URLEncoder.encode("abc def")));

        parsed = AlarmURI.parseAlarmURI(AlarmURI.createURI("host.my.site", "Test", "param=" + URLEncoder.encode("abc def", StandardCharsets.UTF_8) + "&param2=value"));
        assertThat(parsed.length, equalTo(3));
        assertThat(parsed[0], equalTo("host.my.site:9092"));
        assertThat(parsed[1], equalTo("Test"));
        assertThat(parsed[2], equalTo("param=" + URLEncoder.encode("abc def", StandardCharsets.UTF_8) + "&param2=value"));

        try {
            AlarmURI.parseAlarmURI(URI.create("alarm://server_but_no_config"));
            fail("Didn't catch missing config name");
        } catch (Exception ex) {
            // Expected
            assertThat(ex.getMessage(), containsString("expecting"));
        }
    }

    @Test
    public void getRawQueryParametersValues() throws Exception {
        // create with URI.create
        // with / without default port
        //ImmutableMap<String, String> immutableMap = AlarmURI.getRawQueryParametersValues(URI.create("alarm://localhost:9092/Accelerator"));
        Map<String, String> map = AlarmURI.getRawQueryParametersValues(URI.create("alarm://localhost:9092/Accelerator"));
        assertThat(map.get("param"), equalTo(null));

        map = AlarmURI.getRawQueryParametersValues(URI.create("alarm://host.my.site/Test"));
        assertThat(map.get("param"), equalTo(null));

        map = AlarmURI.getRawQueryParametersValues(URI.create("alarm://host.my.site/Test?param"));
        assertThat(map.get("param"), equalTo(null));

        map = AlarmURI.getRawQueryParametersValues(URI.create("alarm://host.my.site/Test?param=value"));
        assertThat(map.get("param"), equalTo("value"));

        map = AlarmURI.getRawQueryParametersValues(URI.create("alarm://host.my.site/Test?param=" + URLEncoder.encode("abc def", StandardCharsets.UTF_8)));
        assertThat(map.get("param"), equalTo(URLEncoder.encode("abc def", StandardCharsets.UTF_8)));

        map = AlarmURI.getRawQueryParametersValues(URI.create("alarm://host.my.site/Test?param=" + URLEncoder.encode("abc def", StandardCharsets.UTF_8) + "&param2=value"));
        assertThat(map.get("param"), equalTo(URLEncoder.encode("abc def", StandardCharsets.UTF_8)));
        assertThat(map.get("param2"), equalTo("value"));

        // create with AlarmURI.createURI
        // with / without default port
        map = AlarmURI.getRawQueryParametersValues(AlarmURI.createURI("localhost:9092", "Accelerator"));
        assertThat(map.get("param"), equalTo(null));

        map = AlarmURI.getRawQueryParametersValues(AlarmURI.createURI("host.my.site", "Test"));
        assertThat(map.get("param"), equalTo(null));

        map = AlarmURI.getRawQueryParametersValues(AlarmURI.createURI("host.my.site", "Test", "param"));
        assertThat(map.get("param"), equalTo(null));

        map = AlarmURI.getRawQueryParametersValues(AlarmURI.createURI("host.my.site", "Test", "param=value"));
        assertThat(map.get("param"), equalTo("value"));

        map = AlarmURI.getRawQueryParametersValues(AlarmURI.createURI("host.my.site", "Test", "param=" + URLEncoder.encode("abc def", StandardCharsets.UTF_8)));
        assertThat(map.get("param"), equalTo(URLEncoder.encode("abc def", StandardCharsets.UTF_8)));

        map = AlarmURI.getRawQueryParametersValues(AlarmURI.createURI("host.my.site", "Test", "param=" + URLEncoder.encode("abc def", StandardCharsets.UTF_8) + "&param2=value"));
        assertThat(map.get("param"), equalTo(URLEncoder.encode("abc def", StandardCharsets.UTF_8)));
        assertThat(map.get("param2"), equalTo("value"));
    }

}
