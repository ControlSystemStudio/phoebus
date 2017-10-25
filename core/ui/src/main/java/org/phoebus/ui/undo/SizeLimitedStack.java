/*******************************************************************************
 * Copyright (c) 2010-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.undo;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/** A stack with limited size.
 *  When full, oldest element will be removed.
 *  @param <T> Stack element
 *  @author Xihui Chen original org.csstudio.swt.xygraph.undo.SizeLimitedStack
 *  @author Kay Kasemir
 */
public class SizeLimitedStack<T>
{
    final private int limit;
    final private LinkedList<T> list = new LinkedList<T>();

    /**@param limit Maximum number of stack elements */
    public SizeLimitedStack(final int limit)
    {
        this.limit = limit;
    }

    /** @return <code>true</code> if stack is empty */
    public boolean isEmpty()
    {
        return list.isEmpty();
    }

    /** @param item Item to pushed onto stack. */
    public void push(final T item)
    {
        if (list.size() >= limit)
            list.removeFirst();
        list.addLast(item);
    }

    /** Get items which are currently on the stack.
     *
     *  <p>For efficiency, this returns a read-only view
     *  that will be affected by changes to the underlying stack,
     *  so the result should not be cached.
     *  @return Items on the stack
     */
    public List<T> getItems()
    {
        return Collections.unmodifiableList(list);
    }

    /** @return Number of items on stack */
    public int size()
    {
        return list.size();
    }

    /** @return Top element
     *  @throws NoSuchElementException if empty
     */
    public T peek()
    {
        return list.getLast();
    }

    /** @return Item removed from top of stack.
     *  @throws NoSuchElementException if this stack is empty
     */
    public T pop()
    {
        return list.removeLast();
    }

    /** Empty the stack */
    public void clear()
    {
        list.clear();
    }
}
