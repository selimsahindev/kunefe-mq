package dev.selimsahin.kunefe.broker.log;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the append-only log engine.
 * <p>
 * Maps to application.yml:
 * <pre>
 * kunefe:
 *   log:
 *     segment:
 *       max-bytes: 67108864
 *       max-hours: 168
 *     retention:
 *       hours: 168
 *       check-interval-ms: 300000
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "kunefe.log")
public class LogConfig {

    private Segment segment = new Segment();
    private Retention retention = new Retention();

    public Segment getSegment() {
        return segment;
    }

    public void setSegment(Segment segment) {
        this.segment = segment;
    }

    public Retention getRetention() {
        return retention;
    }

    public void setRetention(Retention retention) {
        this.retention = retention;
    }

    public static class Segment {

        private long maxBytes = 64 * 1024 * 1024;   // 64MB
        private int maxHours = 168;                 // 7 days

        public long getMaxBytes() {
            return maxBytes;
        }

        public void setMaxBytes(long maxBytes) {
            this.maxBytes = maxBytes;
        }

        public int getMaxHours() {
            return maxHours;
        }

        public void setMaxHours(int maxHours) {
            this.maxHours = maxHours;
        }
    }

    public static class Retention {

        private int hours = 168;                    // 7 days
        private long checkIntervalMs = 300_000L;    // 5 minutes

        public int getHours() {
            return hours;
        }

        public void setHours(int hours) {
            this.hours = hours;
        }

        public long getCheckIntervalMs() {
            return checkIntervalMs;
        }

        public void setCheckIntervalMs(long checkIntervalMs) {
            this.checkIntervalMs = checkIntervalMs;
        }
    }
}