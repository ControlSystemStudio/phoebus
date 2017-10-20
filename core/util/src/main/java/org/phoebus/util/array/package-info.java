/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
/**
 * Set of classes to handle iteration and read-only references to
 * collections and arrays of primitives, without having to code for each
 * individual case.
 * <p>
 * The design is loosely inspired by the Collection framework, but does not
 * directly implement it. Due to the invariant nature of Java generics,
 * it would make the usage of inheritance awkward. See {@link org.phoebus.util.array.IteratorNumber}
 * for more details.
 */
package org.phoebus.util.array;
