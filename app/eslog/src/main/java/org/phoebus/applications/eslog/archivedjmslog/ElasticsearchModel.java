package org.phoebus.applications.eslog.archivedjmslog;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.phoebus.applications.eslog.Activator;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;

/**
 *
 * Default filtering is by time stamp. It is assumed that the used index
 * includes a field of type date that represents this time stamp.
 *
 * @author Michael Ritzert <michael.ritzert@ziti.uni-heidelberg.de>
 */
public class ElasticsearchModel<T extends LogMessage> extends ArchiveModel<T>
{
    /** Number of hits per page when scrolling is used. */
    protected static final int PAGE_SIZE = 1000;

    protected final String dateField;
    protected final String server;
    protected final String index;
    protected final int port;
    protected final String protocol;

    protected Job queryJob;
    protected final Function<Hit<T>, T> converter;
    protected Class<T> parameterType;

    protected List<T> messages;

    @SuppressWarnings("unchecked")
    public ElasticsearchModel(String es_url, String index, String dateField,
            Function<Hit<T>, T> converter) throws MalformedURLException
    {
        Activator.checkParameterString(dateField, "dateField"); //$NON-NLS-1$
        Activator.checkParameterString(index, "index"); //$NON-NLS-1$
        Activator.checkParameterString(es_url, "es_url"); //$NON-NLS-1$
        this.dateField = dateField;
        final var url = new URL(es_url);
        this.index = index;
        this.server = url.getHost();
        final var port = url.getPort();
        this.port = (port != -1) ? port : 9200;
        this.protocol = url.getProtocol();
        this.converter = converter;
        this.parameterType = ((Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    /**
     * Build the query to be sent to the Elasticsearch server.
     *
     * As a default, only filtering by the time stamp and limiting the number of
     * results is implemented. Override to change the query.
     */
    @SuppressWarnings("nls")
    protected Query buildQuery(Instant from, Instant to)
    {
        var query = new Query.Builder();
        var bool = new BoolQuery.Builder().must(getTimeQuery(from, to));
        synchronized (this)
        {
            if (null != this.filters)
            {
                for (PropertyFilter filter : this.filters)
                {
                    if (filter.isInverted())
                    {
                        bool = bool.mustNot(getFilter(filter));
                    }
                    else
                    {
                        bool = bool.must(getFilter(filter));
                    }
                }
            }
        }
        query.bool(bool.build());
        return query.build();
    }

    protected Query getFilter(PropertyFilter filter)
    {
        if (filter instanceof StringPropertyFilter)
        {
            return getFilterQuery((StringPropertyFilter) filter);
        }
        else if (filter instanceof StringPropertyMultiFilter)
        {
            return getFilterQuery((StringPropertyMultiFilter) filter);
        }
        throw new IllegalArgumentException("Filter type not supported."); //$NON-NLS-1$
    }

    protected Query getFilterQuery(StringPropertyFilter filter)
    {
        return MatchQuery.of(
                r -> r.field(filter.getProperty()).query(filter.getPattern()))
                ._toQuery();
    }

    protected Query getFilterQuery(StringPropertyMultiFilter filter)
    {
        final var patterns = Stream.of(filter.getPatterns()).map(FieldValue::of)
                .collect(Collectors.toList());
        final var termsQueryField = new TermsQueryField.Builder()
                .value(patterns).build();
        return TermsQuery
                .of(r -> r.field(filter.getProperty()).terms(termsQueryField))
                ._toQuery();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T[] getMessages()
    {
        synchronized (this)
        {
            if (null != this.messages)
            {
                return this.messages.toArray((T[]) Array
                        .newInstance(this.parameterType, this.messages.size()));
            }
            else
            {
                return (T[]) Array.newInstance(this.parameterType, 0);
            }
        }
    }

    /**
     * Override if your time stamp format cannot be correctly converted to
     * millis since the epoch. E.g. if the time zone information is missing…
     */
    @SuppressWarnings("nls")
    protected Query getTimeQuery(Instant from, Instant to)
    {
        return RangeQuery.of(r -> r.field(this.dateField)
                .gte(JsonData.of(from.toEpochMilli()))
                .lte(JsonData.of(to.toEpochMilli())).format("epoch_millis"))
                ._toQuery();
    }

    @Override
    public void refresh(Instant from, Instant to)
    {
        Activator.checkParameter(from, "from"); //$NON-NLS-1$
        Activator.checkParameter(to, "to"); //$NON-NLS-1$
        synchronized (this)
        {
            // Cancel a job that might already be running
            if (null != this.queryJob) this.queryJob.cancel();
            this.queryJob = JobManager.schedule("ES query", //$NON-NLS-1$
                    new JobRunnable()
                    {
                        @Override
                        public void run(JobMonitor monitor) throws Exception
                        {
                            final var restClient = RestClient
                                    .builder(new HttpHost(
                                            ElasticsearchModel.this.server,
                                            ElasticsearchModel.this.port,
                                            ElasticsearchModel.this.protocol))
                                    .build();
                            final var transport = new RestClientTransport(
                                    restClient, new JacksonJsonpMapper());
                            final var client = new ElasticsearchClient(
                                    transport);

                            final Time keepAlive = Time.of(t -> t.time("60s"));
                            final var query = buildQuery(from, to);
                            final var pitResult = client.openPointInTime(
                                    f -> f.index(index).keepAlive(keepAlive));
                            final var pitId = pitResult.id();

                            final var result = new LinkedList<T>();
                            boolean done = false;
                            while (!done)
                            {
                                final var request = SearchRequest.of(r -> r
                                        .query(query)
                                        .source(sc -> sc.fetch(false))
                                        .pit(p -> p.id(pitId)
                                                .keepAlive(keepAlive))
                                        .size(PAGE_SIZE).from(result.size())
                                        .fields(ff -> ff.field("*"))
                                        .fields(ff -> ff.field(dateField)
                                                .format("epoch_millis")));

                                final var search = client.search(request,
                                        parameterType);
                                result.addAll(search.hits().hits().stream()
                                        .map(ElasticsearchModel.this.converter)
                                        .collect(Collectors.toList()));
                                done = search.hits().total().value() == result
                                        .size();
                            }
                            client.closePointInTime(p -> p.id(pitId));

                            Collections.sort(result);
                            synchronized (ElasticsearchModel.this)
                            {
                                ElasticsearchModel.this.messages = result;
                                ElasticsearchModel.this.queryJob = null;
                            }
                            ElasticsearchModel.this
                                    .sendCompletionNotification();
                        }
                    });
        }
    }
}
