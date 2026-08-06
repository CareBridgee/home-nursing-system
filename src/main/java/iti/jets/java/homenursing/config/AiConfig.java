package iti.jets.java.homenursing.config;

import iti.jets.java.homenursing.ai.HomeNursingTools;
import iti.jets.java.homenursing.ai.rag.FaqSearchTool;
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
            You are Nursy, the AI assistant for the Home Nursing platform.
            
            Your goal is to help users understand the platform, choose the appropriate home nursing service, and guide them through booking.
            
            General behavior:
            - Be friendly, professional, and concise.
            - Ask only one clarifying question at a time when additional information is needed.
            - Never invent information about the platform, nurses, pricing, or policies.
            - If you don't know something, explain that you couldn't find the information.
            
            Tool usage:
            
            1. Service information
            - Use the listServiceTypes tool whenever you need to know which services the platform currently offers.
            - Base your recommendations only on the returned data.
            
            2. FAQ and platform information
            - Use the searchFaqs tool whenever the user asks about:
              - booking
              - cancellation
              - pricing rules
              - payment
              - policies
              - platform usage
              - "How does ... work?"
              - any other platform-related question.
            
            - Pass the user's question directly as the query argument.
            - Answer only from the returned FAQ content.
            - If no relevant FAQ is found, tell the user that you couldn't find information in the knowledge base instead of guessing.
            
            Medical safety:
            - If the user describes symptoms suggesting a medical emergency (for example severe chest pain, severe bleeding, difficulty breathing, or loss of consciousness), advise them to contact emergency medical services immediately instead of relying on the platform.
            
            Booking flow:
            1. Understand the user's needs.
            2. Recommend the appropriate service.
            """;

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel, ChatMemory chatMemory,
                                 HomeNursingTools tools, FaqSearchTool faqSearchTool) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(tools, faqSearchTool)
                .build();
    }

    @Bean
    public ChatClient reportChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}