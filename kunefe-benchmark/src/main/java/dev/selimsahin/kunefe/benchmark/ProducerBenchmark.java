package dev.selimsahin.kunefe.benchmark;

import dev.selimsahin.kunefe.broker.KunefeBrokerApplication;
import dev.selimsahin.kunefe.client.KunefeClient;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for measuring producer throughput.
 * <p>
 * Measures how many messages per second Kunefe MQ can publish
 * to a single topic under various payload sizes.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 10)
@Fork(1)
public class ProducerBenchmark {

    private ConfigurableApplicationContext context;
    private KunefeClient client;

    private static final byte[] SMALL_PAYLOAD = new byte[64];      // 64B
    private static final byte[] MEDIUM_PAYLOAD = new byte[1024];   // 1KB
    private static final byte[] LARGE_PAYLOAD = new byte[10240];   // 10KB

    @Setup(Level.Trial)
    public void setUp() {
        System.setProperty("kunefe.data.dir", "./benchmark-data");
        System.setProperty("kunefe.offset.dir", "./benchmark-offsets");
        System.setProperty("grpc.server.port", "7070");
        System.setProperty("server.port", "8081");

        context = SpringApplication.run(KunefeBrokerApplication.class);
        client = KunefeClient.connect("localhost", 7070);
        client.createTopic("benchmark-topic", 1);
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        client.close();
        context.close();
        deleteDirectory("./benchmark-data");
        deleteDirectory("./benchmark-offsets");
    }

    @Benchmark
    public long publishSmallPayload() {
        return client.producer().send("benchmark-topic", SMALL_PAYLOAD);
    }

    @Benchmark
    public long publishMediumPayload() {
        return client.producer().send("benchmark-topic", MEDIUM_PAYLOAD);
    }

    @Benchmark
    public long publishLargePayload() {
        return client.producer().send("benchmark-topic", LARGE_PAYLOAD);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteDirectory(String path) {
        java.io.File dir = new java.io.File(path);
        if (dir.exists()) {
            for (java.io.File file : Objects.requireNonNull(dir.listFiles())) {
                file.delete();
            }
            dir.delete();
        }
    }
}
