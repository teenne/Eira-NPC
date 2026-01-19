package com.storyteller.llm.providers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.storyteller.llm.LLMProvider;
import com.storyteller.llm.LLMProvider.ChatMessage;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OllamaProvider using MockWebServer
 */
class OllamaProviderTest {

    private MockWebServer mockServer;
    private static final Gson GSON = new Gson();

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    /**
     * Helper to create a configured OllamaProvider with mocked endpoint
     */
    private OllamaProvider createTestableProvider() throws Exception {
        OllamaProvider provider = new OllamaProvider();

        // Use reflection to set the endpoint and model directly
        setField(provider, "endpoint", mockServer.url("").toString().replaceAll("/$", ""));
        setField(provider, "model", "test-model");

        // Create and set the HTTP client
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build();
        setField(provider, "client", client);

        // Set available to true for chat tests
        Field availableField = OllamaProvider.class.getDeclaredField("available");
        availableField.setAccessible(true);
        ((AtomicBoolean) availableField.get(provider)).set(true);

        return provider;
    }

    private void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    @Test
    @DisplayName("getName should return provider name with model")
    void getNameShouldReturnProviderNameWithModel() throws Exception {
        OllamaProvider provider = new OllamaProvider();
        setField(provider, "model", "mistral:7b");

        String name = provider.getName();

        assertEquals("Ollama (mistral:7b)", name);
    }

    @Test
    @DisplayName("isAvailable should return false before initialization")
    void isAvailableShouldReturnFalseBeforeInit() {
        OllamaProvider provider = new OllamaProvider();

        assertFalse(provider.isAvailable());
    }

    @Test
    @DisplayName("chat should return unavailable message when not initialized")
    void chatShouldReturnUnavailableWhenNotInitialized() {
        OllamaProvider provider = new OllamaProvider();

        String response = provider.chat("prompt", List.of()).join();

        assertTrue(response.contains("not available"));
    }

    @Test
    @DisplayName("chat should parse Ollama response format correctly")
    void chatShouldParseOllamaResponseFormat() throws Exception {
        OllamaProvider provider = createTestableProvider();

        // Enqueue mock response
        JsonObject mockResponse = new JsonObject();
        JsonObject message = new JsonObject();
        message.addProperty("role", "assistant");
        message.addProperty("content", "Hello, traveler!");
        mockResponse.add("message", message);

        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody(GSON.toJson(mockResponse)));

        // Make chat request
        List<ChatMessage> messages = List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hi there")
        );

        String response = provider.chat("You are a sage.", messages).join();

        assertEquals("Hello, traveler!", response);
    }

    @Test
    @DisplayName("chat should send correct request format")
    void chatShouldSendCorrectRequestFormat() throws Exception {
        OllamaProvider provider = createTestableProvider();

        // Enqueue mock response
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"message\":{\"content\":\"response\"}}"));

        // Make chat request
        List<ChatMessage> messages = List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hello"),
            new ChatMessage(ChatMessage.Role.ASSISTANT, "Hi"),
            new ChatMessage(ChatMessage.Role.USER, "How are you?")
        );

        provider.chat("You are Eldric.", messages).join();

        // Verify request
        RecordedRequest request = mockServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/api/chat"));

        String body = request.getBody().readUtf8();

        // Verify JSON structure
        JsonObject requestJson = GSON.fromJson(body, JsonObject.class);
        assertEquals("test-model", requestJson.get("model").getAsString());
        assertFalse(requestJson.get("stream").getAsBoolean());

        // Verify messages array
        JsonArray messagesArray = requestJson.getAsJsonArray("messages");
        assertEquals(4, messagesArray.size()); // system + 3 user messages

        // First message should be system
        JsonObject systemMsg = messagesArray.get(0).getAsJsonObject();
        assertEquals("system", systemMsg.get("role").getAsString());
        assertEquals("You are Eldric.", systemMsg.get("content").getAsString());
    }

    @Test
    @DisplayName("chat should include generation options")
    void chatShouldIncludeGenerationOptions() throws Exception {
        OllamaProvider provider = createTestableProvider();

        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"message\":{\"content\":\"test\"}}"));

        provider.chat("prompt", List.of(new ChatMessage(ChatMessage.Role.USER, "hi"))).join();

        RecordedRequest request = mockServer.takeRequest();
        String body = request.getBody().readUtf8();
        JsonObject requestJson = GSON.fromJson(body, JsonObject.class);

        assertTrue(requestJson.has("options"));
        JsonObject options = requestJson.getAsJsonObject("options");
        assertEquals(0.8, options.get("temperature").getAsDouble(), 0.01);
    }

    @Test
    @DisplayName("chat should return fallback on error response")
    void chatShouldReturnFallbackOnError() throws Exception {
        OllamaProvider provider = createTestableProvider();

        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"error\":\"Model not found\"}"));

        String response = provider.chat("prompt", List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hi")
        )).join();

        assertTrue(response.contains("lost in thought"));
    }

    @Test
    @DisplayName("chat should return fallback on HTTP error")
    void chatShouldReturnFallbackOnHttpError() throws Exception {
        OllamaProvider provider = createTestableProvider();

        mockServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"));

        String response = provider.chat("prompt", List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hi")
        )).join();

        assertTrue(response.contains("lost in thought"));
    }

    @Test
    @DisplayName("shutdown should set available to false")
    void shutdownShouldSetAvailableToFalse() throws Exception {
        OllamaProvider provider = createTestableProvider();

        assertTrue(provider.isAvailable());

        provider.shutdown();

        assertFalse(provider.isAvailable());
    }

    @Test
    @DisplayName("chat should handle empty messages list")
    void chatShouldHandleEmptyMessagesList() throws Exception {
        OllamaProvider provider = createTestableProvider();

        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"message\":{\"content\":\"Hello!\"}}"));

        String response = provider.chat("You are a sage.", List.of()).join();

        assertEquals("Hello!", response);

        // Verify request still includes system message
        RecordedRequest request = mockServer.takeRequest();
        String body = request.getBody().readUtf8();
        JsonObject requestJson = GSON.fromJson(body, JsonObject.class);
        JsonArray messagesArray = requestJson.getAsJsonArray("messages");
        assertEquals(1, messagesArray.size()); // Only system message
    }

    @Test
    @DisplayName("chat should handle missing content field gracefully")
    void chatShouldHandleMissingContentField() throws Exception {
        OllamaProvider provider = createTestableProvider();

        // Response without content field
        mockServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setBody("{\"message\":{}}"));

        String response = provider.chat("prompt", List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hi")
        )).join();

        // Should throw NPE internally and return fallback
        assertTrue(response.contains("lost in thought"));
    }

    @Test
    @DisplayName("LLMProvider interface should define correct methods")
    void llmProviderInterfaceShouldDefineCorrectMethods() {
        // Verify OllamaProvider implements all interface methods
        OllamaProvider provider = new OllamaProvider();

        assertNotNull(provider.initialize());
        assertNotNull(provider.chat("", List.of()));
        assertFalse(provider.isAvailable());
        assertNotNull(provider.getName());
        assertDoesNotThrow(provider::shutdown);
    }
}
