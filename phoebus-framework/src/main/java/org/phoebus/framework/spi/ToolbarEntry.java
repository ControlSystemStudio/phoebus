package org.phoebus.framework.spi;

import java.util.concurrent.Callable;

/**
 * 
 * @author Kunal Shroff
 *
 */
public interface ToolbarEntry {

    public String getName();

    public <T> Callable<T> getActions();
}
