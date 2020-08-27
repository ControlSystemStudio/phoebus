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

package org.phoebus.util;

import java.io.File;

public class FileExtensionUtil {

    /**
     *  Enforce a file extension. If the input file has an extension, it is replaced by the wanted one.
     *
     *  @param file File with any or no extension
     *  @param desiredExtension Desired file extension.
     *  @return {@link File} with the desired file extension
     */
    public static File enforceFileExtension(final File file, final String desiredExtension)
    {
        final String path = file.getPath();
        final int sep = path.lastIndexOf('.');
        if (sep < 0){
            return new File(path + "." + desiredExtension);
        }
        final String ext = path.substring(sep + 1);
        if (! ext.equals(desiredExtension)){
            return new File(path.substring(0, sep) + "." + desiredExtension);
        }
        return file;
    }
}
