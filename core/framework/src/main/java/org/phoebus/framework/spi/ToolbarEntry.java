package org.phoebus.framework.spi;

import java.util.concurrent.Callable;

/**
 * 
 * @author Kunal Shroff
 * @param <V>
 *
 */
public interface ToolbarEntry<V> extends Callable<V> {

    public String getName();

}
