package org.phoebus.applications.display.navigation;

import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

/**
 * A utility Class for handling the navigation of .bob files
 */
public class ProcessOPITest {


    @Test(expected = UnsupportedOperationException.class)
    public void testRandomFile()
    {
        File file = new File(getClass().getClassLoader().getResource("random.txt").getFile());
        ProcessOPI processOPI = new ProcessOPI(file);
        Set<File> result = processOPI.process();
    }

    @Test
    public void testEmptyBOBList()
    {
        File file = new File(getClass().getClassLoader().getResource("bob/root_with_no_children.bob").getFile());
        ProcessOPI processOPI = new ProcessOPI(file);
        Set<File> result = processOPI.process();
        assertTrue("Failed to parse a bob screen with no children, expected empty list but found list " + result
                , result.isEmpty());
    }

    @Test
    public void testEmptyOPIList()
    {
        File file = new File(getClass().getClassLoader().getResource("opi/root_with_no_children.opi").getFile());
        ProcessOPI processOPI = new ProcessOPI(file);
        Set<File> result = processOPI.process();
        assertTrue("Failed to parse a bob screen with no children, expected empty list but found list " + result
                , result.isEmpty());
    }
    @Test
    public void testBOBList()
    {
        File file = new File(getClass().getClassLoader().getResource("bob/root.bob").getFile());
        ProcessOPI processOPI = new ProcessOPI(file);
        Set<File> result = processOPI.process();
        // Expected results
        Set<File> expectedFiles = new HashSet<>();
        expectedFiles.add(new File(getClass().getClassLoader().getResource("bob/child_1/child_1.bob").getFile()));
        expectedFiles.add(new File(getClass().getClassLoader().getResource("bob/child_1/grand_child_1_1/grand_child_1_1.bob").getFile()));
        expectedFiles.add(new File(getClass().getClassLoader().getResource("bob/child_2/child_2.bob").getFile()));
        expectedFiles.add(new File(getClass().getClassLoader().getResource("bob/child_3/child_3.bob").getFile()));

        assertThat(result, is(expectedFiles));
    }


    @Test
    public void testOPIList()
    {
        File file = new File(getClass().getClassLoader().getResource("opi/root.opi").getFile());
        ProcessOPI processOPI = new ProcessOPI(file);
        Set<File> result = processOPI.process();
        // Expected results
        Set<File> expectedFiles = new HashSet<>();
        expectedFiles.add(new File(getClass().getClassLoader().getResource("opi/child_1/child_1.opi").getFile()));
        expectedFiles.add(new File(getClass().getClassLoader().getResource("opi/child_1/grand_child_1_1/grand_child_1_1.opi").getFile()));
        expectedFiles.add(new File(getClass().getClassLoader().getResource("opi/child_2/child_2.opi").getFile()));
        expectedFiles.add(new File(getClass().getClassLoader().getResource("opi/child_3/child_3.opi").getFile()));

        assertThat(result, is(expectedFiles));
    }

    @Test
    public void testBOBCyclicLinksList()
    {
        // The child_1 and grandchild_1_1 have a cyclic link. The test ensures that the checking for linked files
        // does avoid entering an infinite loop
        File file = new File(getClass().getClassLoader().getResource("bob/cyclic/cyclic_1.bob").getFile());
        ProcessOPI processOPI = new ProcessOPI(file);
        Set<File> result = processOPI.process();
        // Expected results
        Set<File> expectedFiles = new HashSet<>();
        expectedFiles.add(new File(getClass().getClassLoader().getResource("bob/cyclic/cyclic_2.bob").getFile()));

        assertThat(result, is(expectedFiles));
        // assertTrue("Failed to find the linked files. expected " + " found " , result.c);
    }

    @Test
    public void testOPICyclicLinksList()
    {
        // The child_1 and grandchild_1_1 have a cyclic link. The test ensures that the checking for linked files
        // does avoid entering an infinite loop
        File file = new File(getClass().getClassLoader().getResource("opi/cyclic/cyclic_1.opi").getFile());
        ProcessOPI processOPI = new ProcessOPI(file);
        Set<File> result = processOPI.process();
        // Expected results
        Set<File> expectedFiles = new HashSet<>();
        expectedFiles.add(new File(getClass().getClassLoader().getResource("opi/cyclic/cyclic_2.opi").getFile()));

        assertThat(result, is(expectedFiles));
        // assertTrue("Failed to find the linked files. expected " + " found " , result.c);
    }
}
