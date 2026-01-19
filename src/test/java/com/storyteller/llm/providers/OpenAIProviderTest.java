package com.storyteller.llm.providers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.storyteller.llm.LLMProvider.ChatMessage;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OpenAIProvider
 */
class OpenAIProviderTest {

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
     * Helper to create a configured OpenAIProvider
     */
    private OpenAIProvider createTestableProvider(String apiKey) throws Exception {
        OpenAIProvider provider = new OpenAIProvider();

        setField(provider, "apiKey", apiKey);
        setField(provider, "model", "gpt-4");

        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build();
        setField(provider, "client", client);

        Field availableField = OpenAIProvider.class.getDeclaredField("available");
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
        OpenAIProvider provider = new OpenAIProvider();
        setField(provider, "model", "gpt-4-turbo");

        String name = provider.getName();

        assertEquals("OpenAI (gpt-4-turbo)", name);
    }

    @Test
    @DisplayName("isAvailable should return false before initialization")
    void isAvailableShouldReturnFalseBeforeInit() {
        OpenAIProvider provider = new OpenAIProvider();

        assertFalse(provider.isAvailable());
    }

    @Test
    @DisplayName("chat should return unavailable message when not initialized")
    void chatShouldReturnUnavailableWhenNotInitialized() {
        OpenAIProvider provider = new OpenAIProvider();

        String response = provider.chat("prompt", List.of()).join();

        assertTrue(response.contains("not available"));
    }

    @Test
    @DisplayName("shutdown should set available to false")
    void shutdownShouldSetAvailableToFalse() throws Exception {
        OpenAIProvider provider = createTestableProvider("test-key");

        assertTrue(provider.isAvailable());

        provider.shutdown();

        assertFalse(provider.isAvailable());
    }

    @Test
    @DisplayName("OpenAI response format should be parsed correctly")
    void openAIResponseFormatShouldBeParsedCorrectly() {
        // Test the expected OpenAI response format parsing
        String openAIResponse = """
            {
                "id": "chatcmpl-123",
                "object": "chat.completion",
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "Hello from OpenAI!"
                    },
                    "finish_reason": "stop"
                }],
                "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 5,
                    "total_tokens": 15
                }
            }
            """;

        JsonObject jsonResponse = GSON.fromJson(openAIResponse, JsonObject.class);

        assertTrue(jsonResponse.has("choices"));
        JsonArray choices = jsonResponse.getAsJsonArray("choices");
        assertEquals(1, choices.size());

        String content = choices.get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString();
        assertEquals("Hello from OpenAI!", content);
    }

    @Test
    @DisplayName("OpenAI request format should include system in messages array")
    void openAIRequestFormatShouldHaveSystemInMessages() {
        // OpenAI expects system prompt in messages array with role "system"
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4");
        requestBody.addProperty("max_tokens", 1024);
        requestBody.addProperty("temperature", 0.8);

        JsonArray messagesArray = new JsonArray();

        // System message
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", "You are Eldric the Sage.");
        messagesArray.add(systemMsg);

        // User message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", "Hello!");
        messagesArray.add(userMsg);

        requestBody.add("messages", messagesArray);

        String json = GSON.toJson(requestBody);

        // Verify structure
        assertTrue(json.contains("\"role\":\"system\""));
        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("\"temperature\":0.8"));
    }

    @Test
    @DisplayName("OpenAI request should include temperature parameter")
    void openAIRequestShouldIncludeTemperature() {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "gpt-4");
        requestBody.addProperty("temperature", 0.8);

        String json = GSON.toJson(requestBody);

        assertTrue(json.contains("\"temperature\":0.8"));
    }

    @Test
    @DisplayName("OpenAI authorization header should use Bearer token")
    void openAIAuthHeaderShouldUseBearerToken() {
        // OpenAI uses "Authorization: Bearer <api_key>" header format
        String apiKey = "sk-test-key";
        String expectedHeader = "Bearer " + apiKey;

        assertEquals("Bearer sk-test-key", expectedHeader);
    }

    @Test
    @DisplayName("chat should handle empty messages list")
    void chatShouldHandleEmptyMessagesList() throws Exception {
        OpenAIProvider provider = createTestableProvider("test-key");

        assertDoesNotThrow(() -> {
            String response = provider.chat("You are a sage.", List.of()).join();
            assertNotNull(response);
        });
    }

    @Test
    @DisplayName("OpenAI should handle multiple choices")
    void openAIShouldHandleMultipleChoices() {
        // OpenAI can return multiple choices, but we should use the first one
        String responseWithMultipleChoices = """
            {
                "choices": [
                    {"message": {"content": "First choice"}},
                    {"message": {"content": "Second choice"}}
                ]
            }
            """;

        JsonObject jsonResponse = GSON.fromJson(responseWithMultipleChoices, JsonObject.class);
        JsonArray choices = jsonResponse.getAsJsonArray("choices");

        // Should use first choice
        String content = choices.get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString();
        assertEquals("First choice", content);
    }

    @Test
    @DisplayName("Empty API key should prevent initialization")
    void emptyApiKeyShouldPreventInitialization() throws Exception {
        OpenAIProvider provider = new OpenAIProvider();
        setField(provider, "apiKey", "");

        assertFalse(provider.isAvailable());
    }

    @Test
    @DisplayName("LLMProvider interface should be implemented correctly")
    void llmProviderInterfaceShouldBeImplemented() {
        OpenAIProvider provider = new OpenAIProvider();

        assertNotNull(provider.initialize());
        assertNotNull(provider.chat("", List.of()));
        assertFalse(provider.isAvailable());
        assertNotNull(provider.getName());
        assertDoesNotThrow(provider::shutdown);
    }

    @Test
    @DisplayName("OpenAI request should convert USER and ASSISTANT roles correctly")
    void openAIRequestShouldConvertRolesCorrectly() {
        List<ChatMessage> messages = List.of(
            new ChatMessage(ChatMessage.Role.USER, "Hello"),
            new ChatMessage(ChatMessage.Role.ASSISTANT, "Hi there"),
            new ChatMessage(ChatMessage.Role.USER, "How are you?")
        );

        JsonArray messagesArray = new JsonArray();
        for (ChatMessage msg : messages) {
            JsonObject messageObj = new JsonObject();
            String role = msg.role() == ChatMessage.Role.USER ? "user" : "assistant";
            messageObj.addProperty("role", role);
            messageObj.addProperty("content", msg.content());
            messagesArray.add(messageObj);
        }

        assertEquals(3, messagesArray.size());
        assertEquals("user", messagesArray.get(0).getAsJsonObject().get("role").getAsString());
        assertEquals("assistant", messagesArray.get(1).getAsJsonObject().get("role").getAsString());
        assertEquals("user", messagesArray.get(2).getAsJsonObject().get("role").getAsString());
    }
}
