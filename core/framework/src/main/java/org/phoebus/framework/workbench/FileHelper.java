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

/** Helper for deleting whole directory tree, move file across file systems
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FileHelper
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

    /** Move a file or directory into another directory
     *
     *  <p>Moves a single file into a new directory,
     *  or moves a whole source directory into a new target directory.
     *
     *  @param original File or directory
     *  @param directory Target directory
     *  @throws Exception on error
     */
    public static void move(final File original, final File directory) throws Exception
    {
        if (original.isDirectory())
        {
            final File subdir = new File(directory, original.getName());
            if (! subdir.exists())
                subdir.mkdirs();
            else
                throw new Exception("Cannot move " + original + " into " + directory +
                                    ": Target exists");

            for (File file : original.listFiles())
                move(file, subdir);

            original.delete();
        }
        else
        {
            final File new_file = new File(directory, original.getName());
            Files.move(original.toPath(), new_file.toPath());
        }
    }


    /** Copy a file or directory into another directory
     *
     *  <p>Copy a single file into a new directory,
     *  or copies a whole source directory into a new target directory.
     *
     *  @param original File or directory
     *  @param directory Target directory
     *  @throws Exception on error
     */
    public static void copy(final File original, final File directory) throws Exception
    {
        if (original.isDirectory())
        {
            final File subdir = new File(directory, original.getName());
            if (! subdir.exists())
                subdir.mkdirs();
            else
                throw new Exception("Cannot copy " + original + " into " + directory +
                                    ": Target exists");

            for (File file : original.listFiles())
                copy(file, subdir);
        }
        else
        {
            final File new_file = new File(directory, original.getName());
            Files.copy(original.toPath(), new_file.toPath());
        }
    }
}
