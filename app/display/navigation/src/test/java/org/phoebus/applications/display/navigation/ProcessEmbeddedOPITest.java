package org.phoebus.applications.display.navigation;


import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A utility Class for handling the navigation of .bob files
 */
public class ProcessEmbeddedOPITest {


    @Test
    public void testEmbeddedList()
    {
        // The child_1 and grandchild_1_1 have a cyclic link. The test ensures that the checking for linked files
        // does avoid entering an infinite loop
        File file = new File(getClass().getClassLoader().getResource("bob/embedded/root_embedded.bob").getFile());
        ProcessOPI processOPI = new ProcessOPI(file);
        Set<File> result = processOPI.processEmbedded();

        result.forEach(System.out::println);
        // Expected results
        Set<File> expectedFiles = new HashSet<>();
        expectedFiles.add(new File(getClass().getClassLoader().getResource("bob/embedded/level_1_embedded.bob").getFile()));
        expectedFiles.add(new File(getClass().getClassLoader().getResource("bob/embedded/level_2_embsedded.bob").getFile()));

        assertThat(result, is(expectedFiles));
        // assertTrue("Failed to find the linked files. expected " + " found " , result.c);
    }
}
