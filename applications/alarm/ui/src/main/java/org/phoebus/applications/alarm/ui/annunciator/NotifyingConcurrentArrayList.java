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
    
    /**
     * Set the onAddition runnable.
     * <p> This runnable will be executed on successful addition to the list.
     * @param r
     */
    public void setOnAddition(Runnable r)
    {
        onAddition = r;
    }
    
    /**
     * Set the onRemoval runnable.
     * <p> This runnable will be executed on successful removal from the list.
     * @param r
     */
    public void setOnRemoval(Runnable r)
    {
        onRemoval = r;
    }
    
    /**
     * Add an element to the end of the list.
     * @param toAdd
     */
    public void add(T toAdd)
    {
        array.add(toAdd);
        size++;
        if (onAddition != null)
            onAddition.run();
    }
    
    /**
     * Removes a specified item from the list. 
     * <p>Safe to call if the list does not contain the specified element.
     * @param toRemove
     * @return <code>True</code> if the list contained the element specified to be removed, <code>false</code> otherwise.
     */
    public boolean remove(T toRemove)
    {
        boolean removed = false;
        removed = array.remove(toRemove);
        if (removed)
            size--;
        if (removed && onRemoval != null)
            onRemoval.run();
        return removed;
    }
    
    /**
     * Removes an item from the list at the specified index.
     * @param index
     * @return <code>null</code> if the list is empty, the specified element otherwise.
     * @throws IndexOutOfBoundsException
     */
    public T remove(int index) throws IndexOutOfBoundsException
    {
        if (size > 0)
        {
            if (index > size-1)
                throw new IndexOutOfBoundsException("Index out of range.");
            
            T t = array.remove(index);    
            size--;
            if (onRemoval != null)
                onRemoval.run();
            return t;
        }
        
        return null;
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
     * Gets the first element in the list.
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
    
    /**
     * Pops the first element off the front of the list.
     * @return <code>null</code> if the list is empty, the first element otherwise.
     */
    public T popFront()
    {
        return remove(0);
    }
}
