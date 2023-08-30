/*
 *
 * Copyright (C) 2023 European Spallation Source ERIC.
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

package org.epics.pva.data;

/**
 * Exception for when updating a PVAStructure array with a value that
 * includes a PVAStructure which does not match the element type of the array.
 */
public class ElementTypeException extends Exception {

    /**
     * Constructor returns an exception with a message based on the
     * new Element type and the current element type.
     *
     * @param newElementType New invalid element type
     * @param elementType Current valid element type
     */
    public ElementTypeException(PVAStructure newElementType, PVAStructure elementType) {
        super("Element " + newElementType + " must be of type " + elementType);
    }
}
