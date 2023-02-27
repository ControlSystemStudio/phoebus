package org.phoebus.applications.update;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;

import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/**
 * Pull updates from the gitlab package registry.
 * 
 * <p>
 * The timestamp of the available update is determined from the timestamp of the
 * commit that triggered the pipeline. The version information in the package
 * repository is only used for display purposes.
 *
 * <p>
 * To initialize the package version, use
 * <code>echo org.phoebus.applications.update/current_version=$CI_COMMIT_TIMESTAMP >> phoebus-product/settings.ini</code>
 * in the build pipeline, then package <code>settings.ini</code>.
 * 
 * @author Michael Ritzert
 *
 */
public class GitlabUpdate extends Update implements UpdateProvider
{
    public static final Logger logger = Logger
            .getLogger(GitlabUpdate.class.getPackageName());

    /**
     * The path to the "V4 API".
     * 
     * Typically https://HOST/api/v4
     */
    @Preference
    public static String gitlab_api_url;
    /** The numeric ID of the gitlab project. */
    @Preference
    public static int gitlab_project_id;
    /**
     * The package name used in the registry.
     * 
     * Defaults to "phoebus-$(arch)".
     */
    @Preference
    public static String gitlab_package_name;
    /**
     * Access token for the project's API and registry. Required if access to
     * the gitlab project is not public.
     */
    @Preference
    public static String gitlab_token;

    // filled step by step with the information of the latest update
    private String latest_version;
    private int latest_id;
    private long file_size;
    private String file_name;
    private String latest_commit;

    static
    {
        AnnotatedPreferences.initialize(GitlabUpdate.class,
                "/update_preferences.properties");

        gitlab_package_name = replace_arch(gitlab_package_name);
    }

    @Override
    public boolean isEnabled()
    {
        return !gitlab_api_url.isEmpty() && (gitlab_project_id != 0)
                && !gitlab_package_name.isEmpty();
    }

    @Override
    protected Long getDownloadSize()
    {
        return file_size;
    }

    @Override
    protected InputStream getDownloadStream() throws Exception
    {
        final var endpoint = String.format(
                "projects/%d/packages/generic/%s/%s/%s", gitlab_project_id,
                gitlab_package_name, latest_version, file_name);
        return makeApiCall(endpoint);
    }

    /**
     * Identify the latest commit for the package we want.
     * 
     * Fills in latest_commit, file_size, and file_name.
     * 
     * @throws Exception
     */
    private void getLatestCommit() throws Exception
    {
        monitor.updateTaskName("Finding latest commit.");
        final var endpoint = String.format(
                "projects/%d/packages/%d/package_files", gitlab_project_id,
                latest_id);
        try (final var body = makeApiCall(endpoint);
                final var reader = Json
                        .createReader(new InputStreamReader(body)))
        {
            final var files = reader.readArray();
            // within the package, get the commit id for the last pipeline run.
            final var latest_file = files.stream().map(v -> (JsonObject) v)
                    .sorted((a, b) -> {
                        return -a.getString("created_at")
                                .compareTo(b.getString("created_at"));
                    }).findFirst();
            if (!latest_file.isPresent())
            {
                throw new RuntimeException("No commit found."); //$NON-NLS-1$
            }
            latest_commit = latest_file.get().getJsonArray("pipelines")
                    .getJsonObject(0).getString("sha");
            logger.fine("Latest commit ID: " + latest_commit);
            file_size = latest_file.get().getInt("size");
            file_name = latest_file.get().getString("file_name");
            logger.finer(file_name + " / " + file_size);
        }
    }

    /**
     * Find the latest package with the configured name.
     * 
     * Fills in latest_version and latest_id.
     * 
     * @throws Exception
     */
    private void getLatestPackage() throws Exception
    {
        monitor.updateTaskName("Finding latest package.");
        final var endpoint = String.format(
                "projects/%d/packages?package_name=%s&order_by=version",
                gitlab_project_id, gitlab_package_name);
        try (final var body = makeApiCall(endpoint);
                final var reader = Json
                        .createReader(new InputStreamReader(body)))
        {
            final var packages = reader.readArray();
            // we assume the first package in the list is the latest
            final var latest_package = packages.getJsonObject(0);
            latest_version = latest_package.getString("version");
            logger.info("Latest version: " + latest_version);
            latest_id = latest_package.getInt("id");
        }
    }

    /**
     * Get the timestamp of the given commit.
     * 
     * @param commit_id
     *            The full SHA hash of the commit.
     * @return The timestamp of the commit.
     * @throws Exception
     */
    private Instant getTimestampOfCommit(final String commit_id)
            throws Exception
    {
        monitor.updateTaskName("Getting commit details.");
        final var endpoint = String.format("projects/%d/repository/commits/%s",
                gitlab_project_id, commit_id);
        try (final var body = makeApiCall(endpoint);
                final var reader = Json
                        .createReader(new InputStreamReader(body)))
        {
            final var info = reader.readObject();
            final var timestamp = Instant.parse(info.getString("created_at"));
            logger.fine("Latest release time: " + timestamp);
            return timestamp;
        }
    }

    @Override
    protected Instant getVersion() throws Exception
    {
        getLatestPackage();
        getLatestCommit();
        update_version = getTimestampOfCommit(latest_commit);
        return update_version;
    }

    /**
     * Execute a GET call to the gitlab API.
     * 
     * @param endpoint
     *            The URL to access. The part up to /v4/ is automatically
     *            prepended.
     * @return An InputStream to receive the result.
     * @throws Exception
     */
    private InputStream makeApiCall(final String endpoint) throws Exception
    {
        final var uri = URI
                .create(String.format("%s/%s", gitlab_api_url, endpoint));
        final var c = HttpClient.newHttpClient();
        // perform the HTTP request
        var builder = HttpRequest.newBuilder().uri(uri).GET();
        if (!gitlab_token.isEmpty())
        {
            builder = builder.header("PRIVATE-TOKEN", gitlab_token);
        }
        final var request = builder.build();
        final var response = c.send(request, BodyHandlers.ofInputStream());
        if (200 != response.statusCode())
        {
            try (final var body = response.body())
            {
                // on error, log the response body
                new BufferedReader(
                        new InputStreamReader(body, StandardCharsets.UTF_8))
                                .lines().forEach(logger::warning);
            }
            throw new RuntimeException("API call failed."); //$NON-NLS-1$
        }
        return response.body();
    }
}
