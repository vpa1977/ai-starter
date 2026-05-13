package com.canonical.copyrightagent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Configuration
public class AgentConfig {

    private static final Pattern FRONTMATTER = Pattern.compile(
            "\\A---\\s*\\R.*?\\R---\\s*\\R", Pattern.DOTALL);

    @Bean
    ChatClient copyrightChatClient(
            ChatClient.Builder builder,
            @Value("classpath:/prompts/system-prompt.md") Resource systemPrompt) throws IOException {
        String raw = systemPrompt.getContentAsString(StandardCharsets.UTF_8);
        String prompt = stripFrontmatter(raw);
        return builder.defaultSystem(prompt).build();
    }

    static String stripFrontmatter(String content) {
        return FRONTMATTER.matcher(content).replaceFirst("");
    }
}
