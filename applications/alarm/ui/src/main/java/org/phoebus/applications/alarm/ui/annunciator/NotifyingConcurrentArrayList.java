package org.phoebus.applications.alarm.ui.annunciator;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread safe list that will execute a runnable on addition or removal of elements.
 * @author Evan Smith
 *
 * @param <T>
 */
public class NotifyingConcurrentArrayList<T>
{
    /** Internal thread safe array. */
    final private CopyOnWriteArrayList<T> array = new CopyOnWriteArrayList<T>();
    
    // TODO: Add internal size checks?
    private int size = 0;
    
    /** Runnable to be executed on addition to array. */
    private Runnable onAddition = null;
    /** Runnable to be executed on removal from array. */
    private Runnable onRemoval = null;
    
    public void setOnAddition(Runnable r)
    {
        onAddition = r;
    }
    
    public void setOnRemoval(Runnable r)
    {
        onRemoval = r;
    }
    
    public void add(T toAdd)
    {
        array.add(toAdd);
        size++;
        if (onAddition != null)
            onAddition.run();
    }
    
    public boolean remove(T toRemove)
    {
        boolean removed = false;
        removed = array.remove(toRemove);
        if (removed && onRemoval != null)
            onRemoval.run();
        return removed;
    }
    
    public T remove(int index) throws IndexOutOfBoundsException
    {
        T t = array.remove(index);
        size--;
        if (onRemoval != null)
            onRemoval.run();
        return t;
    }
    
    /**
     * Gets the last element in the list.
     * @return <code>null</code> if the list is empty, the last element otherwise.
     */
    public T back()
    {   
        if (size > 0)
            return array.get(size-1);
        return null;
    }
    
    /**
     * Gets the first element in the list/
     * @return <code>null</code> if empty, the first element otherwise.
     */
    public T front()
    {
        if (size > 0)
            return array.get(0);
        return null;
    }
    
    /**
     * Pops the last element off the back of the list.
     * @return <code>null</code> if the list is empty, the last element otherwise.
     */
    public T popBack()
    {
        return remove(size-1);
    }
}
