package com.canonical.copyrightagent.command;

import com.canonical.copyrightagent.service.ModelService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class CollectLicenseCommand {

    private static final int MAX_CONTENT_BYTES = 8192;

    private final ChatClient chatClient;

    private record LicenseInfo(String text, String holders, String years) {}

    public CollectLicenseCommand(ModelService service) {
        var model = service.createModel("openai/gpt-oss-20b");
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();

        chatClient = ChatClient.builder(model)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Command(name = "collect-license", description = "Collect license headers from files in the specified path")
    public String collectLicense(
            @Option(shortName = 'p', longName = "path", description = "Path to the source directory or file to check", required = true)
            String path,
            @Option(shortName = 'r', longName = "recursive", description = "Recursively check subdirectories", defaultValue = "true")
            boolean recursive) {

        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = recursive ? Files.walk(Path.of(path)) : Files.list(Path.of(path))) {
            paths.filter(Files::isRegularFile).forEach(files::add);
        } catch (IOException e) {
            return "Error walking path '" + path + "': " + e.getMessage();
        }

        if (files.isEmpty()) {
            return "No files found in: " + path;
        }

        Map<String, List<String>> licenseMap = new LinkedHashMap<>();
        Map<String, String> originalLicenseMap = new LinkedHashMap<>();
        List<String> noLicenseFiles = new ArrayList<>();
        var total = files.size();
        while (!files.isEmpty()) {
            var filePath = files.remove(0);
            System.err.println("Processing: " + filePath);
            String content;
            try {
                content = Files.readString(filePath);
            } catch (IOException e) {
                System.err.println("Cannot read file " + filePath + ": " + e.getMessage());
                noLicenseFiles.add(filePath.toString());
                continue;
            }
            if (!content.toLowerCase().contains("copyright")) {
                // TODO: better guard condition, borrow it from decopy
                noLicenseFiles.add(filePath.toString());
                continue;
            }

            var exitingLicense = isExistingLicense(content, licenseMap);
            if (exitingLicense != null) {
                licenseMap.get(exitingLicense).add(filePath.toString());
                continue;
            }

            LicenseInfo licenseInfo = extractLicense(content);
            String licenseText = licenseInfo.text();

            if (licenseText == null || licenseText.isBlank()) {
                noLicenseFiles.add(filePath.toString());
            } else {
                String normalizedKey = normalizeLicense(licenseText);
                originalLicenseMap.putIfAbsent(normalizedKey, licenseText);
                licenseMap.computeIfAbsent(normalizedKey, k -> new ArrayList<>()).add(filePath.toString());
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("License scan results for: ").append(path).append("\n");
        summary.append("Files scanned: ").append(total).append("\n");
        summary.append("Distinct licenses: ").append(licenseMap.size()).append("\n");
        summary.append("Files without license: ").append(noLicenseFiles.size()).append("\n\n");

        int licenseIdx = 1;
        for (Map.Entry<String, List<String>> entry : licenseMap.entrySet()) {
            summary.append("--- License #").append(licenseIdx++).append(" (").append(entry.getValue().size()).append(" files) ---\n");
            summary.append(originalLicenseMap.getOrDefault(entry.getKey(), entry.getKey())).append("\n");
            summary.append("Files:\n");
            for (String f : entry.getValue()) {
                summary.append("  ").append(f).append("\n");
            }
            summary.append("\n");
        }

        if (!noLicenseFiles.isEmpty()) {
            summary.append("--- No License (").append(noLicenseFiles.size()).append(" files) ---\n");
            for (String f : noLicenseFiles) {
                summary.append("  ").append(f).append("\n");
            }
        }

        return summary.toString();
    }

    private String normalizeLicense(String licenseText) {
        return licenseText.replaceAll("[^a-zA-Z0-9]", "");
    }

    private String isExistingLicense(String content, Map<String, List<String>> licenseMap) {
        String normalizedContent = normalizeLicense(content);
        for (String key : licenseMap.keySet()) {
            if (normalizedContent.contains(key)) {
                return key;
            }
        }
        return null;
    }

    private LicenseInfo extractLicense(String fileContent) {
        long  conversationId = System.nanoTime();
        Prompt p = new Prompt("""
                    Examine the file and return the license text verbatim including copyright statement.
                    When asked HOLDERS, Examine the file and return the copyright holders.
                    When asked DATES, Examine the file and return the copyright dates.
                    Return empty text if no license is found.
                    Do not add any explanation, commentary or unrelated markup.
                    File:
                """ + fileContent);
        var chatResponse = chatClient.prompt(p)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call().chatResponse();
        var license = chatResponse.getResult().getOutput().getText();
        if (license == null) {
            return new LicenseInfo(null, null, null);
        }
        chatResponse = chatClient.prompt().user("HOLDERS")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call().chatResponse();
        var holders = chatResponse.getResult().getOutput().getText();
        chatResponse = chatClient.prompt().user("DATES")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call().chatResponse();
        var dates = chatResponse.getResult().getOutput().getText();

        return new LicenseInfo(license, holders, dates);
    }
}
