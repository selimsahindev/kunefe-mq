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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * End-to-end benchmark measuring publish-to-consume latency.
 * <p>
 * Publishes a message and waits until the consumer receives it,
 * measuring the total round-trip time.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 10)
@Fork(1)
public class EndToEndBenchmark {

    private ConfigurableApplicationContext context;
    private KunefeClient client;
    private final AtomicLong fromOffset = new AtomicLong(0);

    private static final byte[] PAYLOAD = new byte[256];

    @Setup(Level.Trial)
    public void setUp() {
        System.setProperty("kunefe.data.dir", "./benchmark-e2e-data");
        System.setProperty("kunefe.offset.dir", "./benchmark-e2e-offsets");
        System.setProperty("grpc.server.port", "7071");
        System.setProperty("server.port", "8082");

        context = SpringApplication.run(KunefeBrokerApplication.class);
        client = KunefeClient.connect("localhost", 7071);
        client.createTopic("e2e-benchmark-topic", 1);
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        client.close();
        context.close();
        deleteDirectory("./benchmark-e2e-data");
        deleteDirectory("./benchmark-e2e-offsets");
    }

    /**
     * Measures end-to-end latency: publish → consume.
     */
    @Benchmark
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public long publishAndConsume() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        long currentOffset = fromOffset.get();

        Thread.ofVirtual().start(() -> client.consumer("benchmark-group", "benchmark-consumer")
                .subscribe("e2e-benchmark-topic", currentOffset, message -> {
                    fromOffset.set(Long.parseLong(message.getMessageId()) + 1);
                    latch.countDown();
                }));

        client.producer().send("e2e-benchmark-topic", PAYLOAD);
        latch.await(5, TimeUnit.SECONDS);

        return fromOffset.get();
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
