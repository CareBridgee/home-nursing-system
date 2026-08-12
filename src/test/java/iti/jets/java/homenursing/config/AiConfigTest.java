package iti.jets.java.homenursing.config;

import iti.jets.java.homenursing.ai.HomeNursingTools;
import iti.jets.java.homenursing.ai.ReservationTools;
import iti.jets.java.homenursing.ai.rag.FaqSearchTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

class AiConfigTest {

    @Test
    void chatClientBeans_areConstructibleWithModelAndMemory() {
        AiConfig config = new AiConfig();

        ChatMemory memory = config.chatMemory();
        assertThat(memory, notNullValue());

        GoogleGenAiChatModel model = mock(GoogleGenAiChatModel.class);
        ChatClient chatClient = config.chatClient(model, memory,
                mock(HomeNursingTools.class), mock(FaqSearchTool.class), mock(ReservationTools.class));
        assertThat(chatClient, notNullValue());

        assertThat(config.reportChatClient(model), notNullValue());
    }
}