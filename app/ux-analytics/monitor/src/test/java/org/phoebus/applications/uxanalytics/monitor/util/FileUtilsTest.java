package org.phoebus.applications.uxanalytics.monitor.util;

import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.phoebus.applications.uxanalytics.monitor.util.FileUtils;
import org.phoebus.framework.preferences.PhoebusPreferenceService;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import static org.phoebus.applications.uxanalytics.monitor.util.FileUtils.WEB_CONTENT_ROOT_ENV_VAR;

public class FileUtilsTest {

    private static String oldWebContentRootSetting = null;
    private static final String resourcesRoot = FileUtilsTest.class.getResource("/test.bob").getFile();
    static File gitDir = null;

    @BeforeAll
    public static void mockWebContentRootSetting(){
        oldWebContentRootSetting = PhoebusPreferenceService.userNodeForClass(FileUtils.class)
                .get(FileUtils.WEB_CONTENT_ROOT_SETTING_NAME, null);
        PhoebusPreferenceService.userNodeForClass(FileUtils.class).put(FileUtils.WEB_CONTENT_ROOT_SETTING_NAME, "http://myserver/bobfiles/");
        //make a directory called .git in the resources directory
        File resourcesDir = new File(resourcesRoot).getParentFile();
        gitDir = new File(resourcesDir, ".git");
        if(!gitDir.exists()){
            gitDir.mkdir();
        }
    }

    @AfterAll
    public static void restoreWebContentRootSetting() {
        if (oldWebContentRootSetting != null) {
            PhoebusPreferenceService.userNodeForClass(FileUtils.class).put(FileUtils.WEB_CONTENT_ROOT_SETTING_NAME, oldWebContentRootSetting);
        }
        if(gitDir.exists()){
            gitDir.delete();
        }
    }

    @Test
    public void testSourceRootOfURL(){
        String url = "http://myserver/bobfiles/something/test.bob";
        String expected = "myserver/bobfiles/";
        String result = FileUtils.findSourceRootOf(url);
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testGetPathWithoutSourceRootWeb(){
        String url = "http://myserver/bobfiles/something/test.bob";
        String expected = "something/test.bob";
        String result = FileUtils.getPathWithoutSourceRoot(url);
        Assertions.assertEquals(expected, result);
        String badUrl = "http://somethingelse/something/test.bob";
        String badResult = FileUtils.getPathWithoutSourceRoot(badUrl);
        Assertions.assertNull(badResult);
    }

    @Test
    public void testSourceRootLocalFileSystem(){
        String path = FileUtilsTest.class.getResource("/test.bob").getPath();
        // .git/../ is the source root, go up one more level to preserve source root when recording
        String expected = gitDir.getParentFile().getParentFile().getPath();
        String result = FileUtils.findSourceRootOf(path);
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testGetPathWithoutSourceRootLocalFileSystem(){
        String path = FileUtilsTest.class.getResource("/test.bob").getPath();
        // .git/../ is the source root, go up one more level to preserve source root when recording
        String expected = "test-classes/test.bob";
        String result = FileUtils.getPathWithoutSourceRoot(path);
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testSHA256Calculation(){
        //calculated using GNU coreutils implementation of sha256sum
        final String expected = "4ab717e75c8a1285d87a651939cb1aa5ab2a5d2e3c7aeeec2e3bb392d3825992";
        String path = FileUtilsTest.class.getResource("/test.bob").getPath();
        String got = FileUtils.getFileSHA256(path);
        Assertions.assertEquals(expected, got);
    }

    @Test
    public void testAnalyticsPathResolution(){
        String path = FileUtilsTest.class.getResource("/test.bob").getPath();
        String expected = "test-classes/test.bob_4ab717e7";
        String got = FileUtils.getAnalyticsPathFor(path);
        Assertions.assertEquals(expected, got);
    }

    @Test
    public void testPathOffOfSourceTreeReturnsNull(){
        String bogusPath = "/something/else/idk/test.bob";
        Assertions.assertNull(FileUtils.getAnalyticsPathFor(bogusPath));
    }
}
