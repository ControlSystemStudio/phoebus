/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.workbench;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** Helper for deleting directory tree
 *  @author Kay Kasemir
 */
public class DirectoryDeleter
{
    /** Delete directory tree
     *
     *  <p>Deletes given file or directory.
     *  In case of directory, deletes all sub-directories.
     *  @param directory Directory (or file) to delete
     *  @throws Exception on error
     */
    public static void delete(final File directory) throws Exception
    {
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(final Path file,
                                             final BasicFileAttributes attr) throws IOException
            {
                // Delete a plain file
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir,
                                                      final IOException ex) throws IOException
            {
                // Delete the directory _after_ the files in the directory have been deleted.
                // XXX For NFS-mounted directory tree, this may fail with DirectoryNotEmptyException
                // https://stackoverflow.com/questions/46144529/java-nio-treevisitor-delete-problems-with-nfs
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
