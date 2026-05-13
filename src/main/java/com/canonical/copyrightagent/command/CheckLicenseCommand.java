package com.canonical.copyrightagent.command;


import com.canonical.copyrightagent.service.ModelService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
public class CheckLicenseCommand {

    private final ChatClient chatClient;

    public CheckLicenseCommand(ModelService service) {
        var model = service.createModel("nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free");
        chatClient = ChatClient.builder(model).build();
    }

    @Command(name =  "check-license", description = "Check license headers in the specified path")
    public String checkLicense(
            @Option(shortName = 'p', longName = "path", description = "Path to the source directory or file to check", required = true)
            String path,
            @Option(shortName = 'r', longName = "recursive", description = "Recursively check subdirectories")
            boolean recursive) {
        var content = chatClient.prompt("Say hi").call().content();
        return "The client said: "+ content;
    }


}
