package dev.selimsahin.kunefe.broker.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Background service responsible for enforcing retention policy.
 * <p>
 * Periodically scans all topic logs and deletes segments that have
 * exceeded the configured retention period. Only inactive segments
 * are eligible for deletion — the active segment is never touched.
 * <p>
 * Runs on a fixed delay configured via kunefe.log.retention.check-interval-ms.
 * Default: every 5 minutes.
 */
@Component
@EnableScheduling
public class RetentionManager {

    private static final Logger log = LoggerFactory.getLogger(RetentionManager.class);

    private final LogManager logManager;
    private final LogConfig logConfig;

    public RetentionManager(LogManager logManager, LogConfig logConfig) {
        this.logManager = logManager;
        this.logConfig = logConfig;
    }

    /**
     * Runs retention cleanup on a fixed delay.
     * <p>
     * fixedDelayString reads from application.yml — fully configurable.
     * Delay starts after the previous execution completes, preventing
     * overlapping cleanup runs.
     */
    @Scheduled(fixedDelayString = "${kunefe.log.retention.check-interval-ms:300000}")
    public void runRetention() {
        log.debug("Running retention cleanup...");

        List<String> topics = logManager.listTopics();
        int totalDeleted = 0;

        for (String topic : topics) {
            try {
                totalDeleted += cleanTopic(topic);
            } catch (IOException e) {
                log.error("Failed to run retention for topic '{}'", topic, e);
            }
        }

        if (totalDeleted > 0) {
            log.info("Retention cleanup complete — deleted {} segment(s)", totalDeleted);
        } else {
            log.debug("Retention cleanup complete — nothing to delete");
        }
    }

    /**
     * Cleans expired segments for a single topic.
     *
     * @return the number of segments deleted
     */
    private int cleanTopic(String topic) throws IOException {
        TopicLog topicLog = logManager.getTopicLog(topic);
        if (topicLog == null) {
            return 0;
        }

        List<LogSegment> expired = new ArrayList<>();
        int retentionHours = logConfig.getRetention().getHours();

        for (LogSegment segment : topicLog.getSegments()) {
            if (segment.isExpired(retentionHours)) {
                expired.add(segment);
            }
        }

        for (LogSegment segment : expired) {
            log.info("Deleting expired segment '{}' from topic '{}'",
                    segment.getPath().getFileName(), topic);
            segment.delete();
            topicLog.removeSegment(segment);
        }

        return expired.size();
    }
}