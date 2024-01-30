package org.phoebus.service.saveandrestore.web.controllers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * {@link Configuration} class setting up a {@link CommonsRequestLoggingFilter}.
 */
@Configuration
public class RequestLoggingFilterConfig {

    /**
     *
     * @return CommonsRequestLoggingFilter used to log client requests.
     */
    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter
                = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(false);
        filter.setAfterMessagePrefix("REQUEST DATA : ");
        return filter;
    }
}