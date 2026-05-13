package com.canonical.copyrightagent.command;


import com.canonical.copyrightagent.agent.CopyrightAgent;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
public class ChatCommand {

    private final CopyrightAgent agent;

    public ChatCommand(CopyrightAgent agent) {
        this.agent = agent;
    }

    @Command(name = "chat", description = "Send a message to the model and print its reply")
    public String chat(
            @Option(shortName = 'm', longName = "message", description = "Message to send to the model", required = true)
            String message,
            @Option(shortName = 'v', longName = "verbose", description = "Stream full output of every command the agent runs")
            boolean verbose) {

        return agent.run(message, verbose);
    }
}
