package org.phoebus.framework.spi;

import java.util.List;
import java.util.concurrent.Callable;

import org.phoebus.framework.selection.Selection;

/**
 * Another example interface which extends {@link Callable}
 * 
 * @author Kunal Shroff
 * @param <V>
 *
 */
public interface ContextMenuEntry<V> {

    public String getName();

    public Object getIcon();

    public List<Class> getSupportedTypes();

    public V callWithSelection(Selection selection) throws Exception;
}
