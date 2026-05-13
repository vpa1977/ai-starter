package com.canonical.copyrightagent.command;

import com.canonical.copyrightagent.service.ModelService;
import com.canonical.copyrightagent.tools.PeekFileTool;
import jakarta.annotation.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Component
public class CollectLicenseCommand {

    record CopyrightID( String path, String copyrightHolder, int startYear, int endYear, String licenseSpdx) {

    }

    private final ChatClient chatClient;

    public CollectLicenseCommand(ModelService service) {
        var model = service.createModel("openai/gpt-oss-20b");
        chatClient = ChatClient.builder(model)
                .build();
    }

    @Command(name = "collect-license", description = "Collect license headers from files in the specified path")
    public String collectLicense(
            @Option(shortName = 'p', longName = "path", description = "Path to the source directory or file to check", required = true)
            String path,
            @Option(shortName = 'r', longName = "recursive", description = "Recursively check subdirectories", defaultValue = "true")
            boolean recursive ) {

        List<String> results = new ArrayList<>();
        StringBuilder files = new StringBuilder();
        AtomicLong counter = new AtomicLong(0);
        try (Stream<Path> paths = recursive ? Files.walk(Path.of(path)) : Files.list(Path.of(path))) {
            paths.filter(Files::isRegularFile)
                 .forEach(filePath -> {
                     if (files.length() > 0) {
                         files.append(",");
                     }
                     files.append(filePath);
                     counter.incrementAndGet();
                     if (counter.get() % 20 != 0) {
                         return;
                     }
                     System.err.println("prompting");
                     var response = promptCopyright( files);
                     files.setLength(0);
                     System.err.println("I have " + response.size());
                     for (var r : response) {
                         String entry = r.path() + "|" + r.copyrightHolder() + "|" + r.startYear() + "|" + r.endYear() + "|" + r.licenseSpdx();
                         results.add(entry);
                     }
                 });

            if (!files.isEmpty()) {
                System.err.println("prompting");
                var response = promptCopyright(files);
                files.setLength(0);
                System.err.println("I have " + response.size());
                for (var r : response) {
                    String entry = r.path() + "|" + r.copyrightHolder() + "|" + r.startYear() + "|" + r.endYear() + "|" + r.licenseSpdx();
                    results.add(entry);
                }
            }
        } catch (IOException e) {
            return "Error walking path '" + path + "': " + e.getMessage();
        }

        if (results.isEmpty()) {
            return "No files found in: " + path;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("License scan results for: ").append(path).append("\n");
        summary.append("Recursive: ").append(recursive).append("\n");
        summary.append("Files scanned: ").append(results.size()).append("\n");
        summary.append("---\n");
        for (String result : results) {
            summary.append(result).append("\n");
        }

        return summary.toString();
    }

    @Nullable
    private List<CopyrightID> promptCopyright(StringBuilder files) {
        var callbacks = ToolCallbacks.from(new PeekFileTool());

        HashMap<String, ToolCallback> toolCallbacks = new HashMap<>();
        for (var c : callbacks) {
            toolCallbacks.put(c.getToolDefinition().name(), c);
        }

        ToolCallingManager toolCallingManager = ToolCallingManager.builder()
                .toolCallbackResolver(new ToolCallbackResolver() {

                    @Override
                    public ToolCallback resolve(String toolName) {
                        int idx = toolName.indexOf('<');
                        if (idx > 0) {
                            toolName = toolName.substring(0, idx);
                        }
                        return toolCallbacks.get(toolName);
                    }
                })
                .build();

        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(callbacks)
                .internalToolExecutionEnabled(false)
                .build();
        Prompt prompt = new Prompt(String.format("""
                Read the file header for each file from the path list %s using the peek tool.
                Check if the file contains a license text block in its header (look for keywords like
                'Copyright', 'License', 'SPDX', 'Apache', 'MIT', 'GPL', 'BSD', 'MPL', 'LGPL', etc.).
                If a license is found, extract the copyright holder name and the year(s).
                Return path, copyright holder, start Year, end year and SPDX license identifier as comma separated values.
                Return none if not license found.
                Do not output anything else.
                """, files.toString()), chatOptions );

        var spec = chatClient.prompt(prompt).call();
        ChatResponse chatResponse = spec.chatResponse();

        while (chatResponse.hasToolCalls()) {
            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);

            prompt = new Prompt(toolExecutionResult.conversationHistory(), chatOptions);
            spec = chatClient.prompt(prompt).call();
            chatResponse = spec.chatResponse();
        }
        String csv = chatResponse.getResult().getOutput().getText();

        List<CopyrightID> ret = new ArrayList<>();
        if ("none".equals(csv) || csv == null) {
            return ret;
        }
        for (String line : csv.lines().toList()) {
            String trimmed = line.strip();
            if (trimmed.isBlank() || trimmed.equalsIgnoreCase("none")) {
                continue;
            }
            String[] parts = trimmed.split(",", 5);
            if (parts.length < 5) {
                continue;
            }
            try {
                ret.add(new CopyrightID(
                        parts[0].strip(),
                        parts[1].strip(),
                        Integer.parseInt(parts[2].strip()),
                        Integer.parseInt(parts[3].strip()),
                        parts[4].strip()
                ));
            } catch (NumberFormatException ignored) {
            }
        }
        return ret;
    }
}
