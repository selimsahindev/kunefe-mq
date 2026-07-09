package dev.selimsahin.kunefe.test;

import dev.selimsahin.kunefe.broker.KunefeBrokerApplication;
import dev.selimsahin.kunefe.client.KunefeClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * Base class for Kunefe integration tests.
 * <p>
 * Starts a real broker instance using Spring Boot test context,
 * and provides a KunefeClient connected to it.
 * <p>
 * Uses @TestInstance(PER_CLASS) to allow @BeforeAll and @AfterAll
 * on non-static methods, enabling access to injected fields.
 */
@SpringBootTest(
        classes = KunefeBrokerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    protected KunefeClient client;

    @BeforeAll
    void setUpClient() {
        client = KunefeClient.connect("localhost", 9099);
    }

    @AfterAll
    void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
        cleanUpTestData();
    }

    /**
     * Removes test data directories after each test class.
     * Ensures test isolation — each test class starts with a clean slate.
     */
    private void cleanUpTestData() throws IOException {
        deleteDirectory(Paths.get("./kunefe-test-data"));
        deleteDirectory(Paths.get("./kunefe-test-offsets"));
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            // ignore
                        }
                    });
        }
    }
}