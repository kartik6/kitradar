package com.kitradar.ingest.dotpe;

import com.kitradar.ingest.AbstractStoreScraper;
import com.kitradar.ingest.HostRequestLimiter;
import com.kitradar.ingest.ScrapeException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BuildIdResolver extends AbstractStoreScraper {

    private static final Pattern BUILD_ID = Pattern.compile("\\\"buildId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    public BuildIdResolver(RestClient storeRestClient, HostRequestLimiter limiter) {
        super(storeRestClient, limiter);
    }

    public String resolve(String baseUrl) {
        Matcher matcher = BUILD_ID.matcher(getText(URI.create(baseUrl)));
        if (!matcher.find()) {
            throw new ScrapeException("Could not find Next.js build ID on " + baseUrl, null);
        }
        return matcher.group(1);
    }

}
