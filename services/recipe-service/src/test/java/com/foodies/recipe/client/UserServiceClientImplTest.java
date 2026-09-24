package com.foodies.recipe.client;

import com.foodies.recipe.client.impl.UserServiceClientImpl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the real timeout (JdkClientHttpRequestFactory) and retry
 * (@Retryable AOP proxy) wiring against a real HTTP server — mocking
 * RestClient directly would prove nothing about either mechanism.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = UserServiceClientImplTest.TestConfig.class)
@TestPropertySource(properties = "user-service.retry.max-attempts=3")
class UserServiceClientImplTest {

    private static final int PORT = 18089;
    private static final String AUTH_HEADER = "Bearer some-token";

    private MockWebServer mockWebServer;

    @Autowired
    private UserServiceClient userServiceClient;

    @BeforeEach
    void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(PORT);
    }

    @AfterEach
    void stopServer() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getCurrentUser_whenServerFailsTwiceThenSucceeds_retriesAndReturnsResult() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"id\":1,\"name\":\"Alice\",\"email\":\"alice@test.com\"}")
                .addHeader("Content-Type", "application/json"));

        UserDto result = userServiceClient.getCurrentUser(AUTH_HEADER);

        assertThat(result.name()).isEqualTo("Alice");
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void getCurrentUser_whenAlwaysFails_exhaustsRetriesAndThrows() {
        for (int i = 0; i < 3; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        }

        assertThatThrownBy(() -> userServiceClient.getCurrentUser(AUTH_HEADER))
                .isInstanceOf(UserServiceUnavailableException.class);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void getCurrentUser_whenUnauthorized_doesNotRetry() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        assertThatThrownBy(() -> userServiceClient.getCurrentUser(AUTH_HEADER))
                .isInstanceOf(UserAuthenticationException.class);

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void getCurrentUser_whenResponseTooSlow_timesOutAfterRetriesAndThrows() {
        for (int i = 0; i < 3; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"id\":1,\"name\":\"Alice\",\"email\":\"alice@test.com\"}")
                    .setHeadersDelay(2, TimeUnit.SECONDS));
        }

        long start = System.currentTimeMillis();

        assertThatThrownBy(() -> userServiceClient.getCurrentUser(AUTH_HEADER))
                .isInstanceOf(UserServiceUnavailableException.class);

        long elapsedMs = System.currentTimeMillis() - start;
        assertThat(elapsedMs).isLessThan(15_000);
    }

    @Configuration
    @EnableRetry
    static class TestConfig {

        @Bean
        static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        RestClient userServiceRestClient() {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(500))
                    .build();
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(Duration.ofMillis(500));

            return RestClient.builder()
                    .baseUrl("http://localhost:" + PORT)
                    .requestFactory(requestFactory)
                    .build();
        }

        @Bean
        UserServiceClient userServiceClient(RestClient userServiceRestClient) {
            return new UserServiceClientImpl(userServiceRestClient);
        }
    }
}
