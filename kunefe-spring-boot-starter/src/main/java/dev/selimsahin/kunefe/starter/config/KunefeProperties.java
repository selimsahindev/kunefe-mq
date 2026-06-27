package dev.selimsahin.kunefe.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Kunefe client auto-configuration.
 * <p>
 * Maps to application.yml:
 * <pre>
 * kunefe:
 *   broker:
 *     host: localhost
 *     port: 6565
 * </pre>
 */
@ConfigurationProperties(prefix = "kunefe")
public class KunefeProperties {

    private Broker broker = new Broker();

    public Broker getBroker() {
        return broker;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    public static class Broker {

        private String host = "localhost";
        private int port = 6565;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}