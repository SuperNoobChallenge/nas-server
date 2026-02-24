package io.github.supernoobchallenge.nasserver.global.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

import java.util.Locale;

public class HibernateBatchPollingSqlTurboFilter extends TurboFilter {

    private static final String HIBERNATE_SQL_LOGGER = "org.hibernate.SQL";

    @Override
    public FilterReply decide(
            Marker marker,
            Logger logger,
            Level level,
            String format,
            Object[] params,
            Throwable t
    ) {
        if (logger == null || format == null) {
            return FilterReply.NEUTRAL;
        }

        if (!HIBERNATE_SQL_LOGGER.equals(logger.getName())) {
            return FilterReply.NEUTRAL;
        }

        String normalized = format.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        if (isBatchPollingQuery(normalized)) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }

    private boolean isBatchPollingQuery(String normalizedSql) {
        return normalizedSql.contains("frombatch_job_queues")
                && normalizedSql.contains("next_run_at<=")
                && normalizedSql.contains("statusin");
    }
}
