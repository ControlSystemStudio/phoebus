/**
 * Copyright (C) 2010-2012 Brookhaven National Laboratory
 * Copyright (C) 2010-2012 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
 * All rights reserved. Use is subject to license terms.
 */
package org.phoebus.channelfinder;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * An observable query to channel finder that maintains the cached result.
 * 
 * @author carcassi
 */
public class ChannelQuery {

	private final ChannelFinderClient client;
	private final String query;
	private static Executor defaultQueryExecutor = Executors.newSingleThreadExecutor();
	private final Executor queryExecutor;
	
	
	/**
	 * A new query with the given search string.
	 * 
	 * @param query the query; cannot be null
	 * @return a new builder
	 */
	public static Builder query(String query) {
		return new Builder(query);
	}

	
	/**
	 * The executor on which the queries are executed.
	 * 
	 * @return the current executor
	 */
	public static Executor getDefaultQueryExecutor() {
		return defaultQueryExecutor;
	}
	
	/**
	 * Change the executor on which the queries are executed.
	 * <p>
	 * TODO: it's not clear who has the responsibility of closing the old executor
	 * 
	 * @param defaultQueryExecutor the new executor
	 */
	public static void setDefaultQueryExecutor(Executor defaultQueryExecutor) {
		if (defaultQueryExecutor == null)
			throw new NullPointerException("Executor can't be null");
		ChannelQuery.defaultQueryExecutor = defaultQueryExecutor;
	}
	
	/**
	 * Result of the query. Groups both result and error so that it's an immutable
	 * and atomic combination.
	 * 
	 * @author carcassi
	 */
	public static class Result {
		public final Exception exception;	
		public final Collection<Channel> channels;
		
		public Result(Exception exception, Collection<Channel> channels) {
			this.exception = exception;
			this.channels = channels;
		}
		
	}
	
	private volatile Result result;
	// Guarded by this: will keep track whether a query is already running
	private boolean running = false;

	private List<ChannelQueryListener> listeners = new CopyOnWriteArrayList<ChannelQueryListener>();

	public static class Builder {
		private String query = null;
		private ChannelFinderClient client = ChannelFinderService.getInstance().getClient();
		private Executor queryExecutor = defaultQueryExecutor;
		private Result result = null;

		private Builder(String query) {
			if (query == null)
				throw new IllegalArgumentException(
						"query string cannot be null");
			this.query = query;
		}

		/**
		 * Changes which client should be used to execute the query.
		 * 
		 * @param client a cliemt
		 * @return this
		 */
		public Builder using(ChannelFinderClient client) {
			if (client == null)
				throw new NullPointerException("Client can't be null");
			this.client = client;
			return this;
		}
		
		/**
		 * Pre-fills the cached result with the given channels and exception.
		 * 
		 * @param channels the result of the query
		 * @param exception the exception for the result; can be null
		 * @return this
		 */
		public Builder result(Collection<Channel> channels, Exception exception) {
			result = new Result(exception, channels);
			return this;
		}

		/**
		 * Changes which executor should execute the query.
		 * 
		 * @param executor an executor
		 * @return this
		 */
		public Builder on(Executor executor) {
			if (executor == null)
				throw new NullPointerException("Executor can't be null");
			this.queryExecutor = executor;
			return this;
		}

		/**
		 * Creates the new query. The query is not executed until
		 * is needed.
		 * 
		 * @return a new query
		 */
		public ChannelQuery build() {
			return new ChannelQuery(this.query, this.client, this.queryExecutor, this.result);
		}
	}

	private ChannelQuery(String query, ChannelFinderClient client, Executor queryExecutor, Result result) {
		super();
		this.query = query;
		this.client = client;
		this.queryExecutor = queryExecutor;
		this.result = result;
	}

	/**
	 * Adds a new listener that is called every time the query is executed.
	 * Note: if you want the listener to be called at least once,
	 * use {@link #execute(ChannelQueryListener)}.
	 * 
	 * @param listener a new listener
	 */
	public void addChannelQueryListener(ChannelQueryListener listener) {
		this.listeners.add(listener);
	}

	/**
	 * Removes a listener.
	 * 
	 * @param listener the listener to be removed
	 */
	public void removeChannelQueryListener(ChannelQueryListener listener) {
		this.listeners.remove(listener);
	}

	private void fireGetQueryResult(Result result) {
		for (ChannelQueryListener listener : this.listeners) {
			listener.queryExecuted(result);
		}
	}
	
	/**
	 * The text of the query.
	 * 
	 * @return the query text
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * The result of the query, if present.
	 * 
	 * @return result or null if the query was never executed
	 */
	public Result getResult() {
		return this.result;
	}
	
	/**
	 * Executes the query and calls the listener with the result.
	 * If the query was already executed, the listener is called
	 * immediately with the result.
	 * 
	 * @param listener - channel query listener
	 */
	public void execute(ChannelQueryListener listener) {
		addChannelQueryListener(listener);
		
		// Make a local copy to avoid synchronization
		Result localResult = result;
		
		// If the query was executed, just call the listener
		if (localResult != null) {
			listener.queryExecuted(localResult);
		} else {
			execute();
		}
		
	}
	
	/**
	 * Triggers a new execution of the query, and calls
	 * all the listeners as a result.
	 */
	public void refresh() {
		execute();
	}

	private void execute() {
		// If it's already running, do nothing
		synchronized (this) {
			if (running)
				return;
			running = true;
		}
		
		queryExecutor.execute(new Runnable() {

			@Override
			public void run() {
				Result localResult = null;
				try {
					Collection<Channel> channels = client.find(query);
					localResult = new Result(null, channels);
				} catch (Exception e) {
					localResult = new Result(e, null);
				} finally {
					result = localResult;
					synchronized (this) {
						running = false;
					}
					fireGetQueryResult(localResult);
				}
			}
		});
	}
	
	@Override
	public int hashCode() {
		return getQuery().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ChannelQuery) {
			return query.equals(((ChannelQuery) obj).getQuery());
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return getQuery();
	}

}
