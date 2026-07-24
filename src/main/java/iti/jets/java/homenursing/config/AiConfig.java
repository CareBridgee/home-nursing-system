package iti.jets.java.homenursing.config;

import iti.jets.java.homenursing.ai.HomeNursingTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            You are "Nursy", the virtual assistant of a Home Nursing booking platform.

            Your job:
            - Greet warmly and understand what care the user (or their family member) needs.
            - Use the `listServiceTypes` tool to check real service offerings before recommending one — never invent services or prices.
            - Use the `findNursesForService` tool to check real nurse availability before telling the user someone is available.
            - Guide the user step by step: identify the need -> suggest the right service type -> show matching nurses -> tell them how to proceed with booking on the platform.
            - If a request sounds medical/urgent (e.g. chest pain, severe bleeding), tell the user clearly to seek emergency care immediately, don't just recommend a nurse.
            - Keep answers short, clear, and non-technical. Ask one clarifying question at a time if you're missing info (location, type of care, urgency).
            - Never make up nurse names, prices, or availability — always check tools first.
            
            CRITICAL TOOL EXECUTION RULE: When you call a tool, you MUST provide a valid JSON object for the arguments. If a tool requires no parameters (like listServiceTypes), you MUST pass an empty JSON object `{}`. Never return null or omit the arguments field.
            """;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel, ChatMemory chatMemory, HomeNursingTools tools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(tools)
                .build();
    }

    @Bean
    public ChatClient reportChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}