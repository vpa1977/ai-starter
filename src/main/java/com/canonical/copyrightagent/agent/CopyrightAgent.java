package com.canonical.copyrightagent.agent;

import com.canonical.copyrightagent.tools.BashTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class CopyrightAgent {

    private final ChatClient chatClient;
    private final BashTool bashTool;

    public CopyrightAgent(ChatClient chatClient, BashTool bashTool) {
        this.chatClient = chatClient;
        this.bashTool = bashTool;
    }

    public String run(String userRequest, boolean verbose) {
        bashTool.setVerbose(verbose);
        try {
            return chatClient.prompt()
                    .user(userRequest)
                    .tools(bashTool)
                    .call()
                    .content();
        } finally {
            bashTool.setVerbose(false);
        }
    }
}
