package com.canonical.copyrightagent.command;

import com.canonical.copyrightagent.service.CommentExtractor;
import com.canonical.copyrightagent.service.CommentExtractorImpl;
import com.canonical.copyrightagent.service.ModelService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class CollectLicenseCommand {


    @Autowired
    private CommentExtractor extractor;

    private static final int MAX_CONTENT_BYTES = 8192;

    private final ChatClient chatClient;
    private final int MAX_LICENSE_LENGTH = 32 * 1024;

    private record LicenseInfo(String text, String holders, String years, boolean full) {}
    private record LicensedFiles(LicenseInfo info, ArrayList<String> files) {}
    public CollectLicenseCommand(ModelService service) {
        //var model = service.createModel("liquid/lfm-2-24b-a2b");
        //var model = service.createModel("meta-llama/llama-3.1-8b-instruct");
        var model = service.createModel("google/gemma-4-e4b");
        //var model = service.createModel("nvidia/nemotron-3-nano-4b");
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();

        chatClient = ChatClient.builder(model)
                .defaultSystem("""
                        # Your task
                        Find full license text, copyright holders, copyright years from user prompt.
                        if license not found:
                        - Answer NO
                        if license found:
                        - Answer with yaml:
                        ```yaml
                        license: |
                          license-text
                        holders: |
                          copyright-holders
                        years: |
                          copyright-years
                        ```
                        DO NOT ANSWER ANYTHING ELSE
       
                        # Examples
                        ---
                        
                        ### Example 1: MIT License Found (Standard Block Comment)
                        
                        **User Prompt:**
                        
                        ```python
                        ""\"
                        Copyright (c) 2024 Jane Doe, John Smith
                        
                        Permission is hereby granted, free of charge, to any person obtaining a copy
                        of this software and associated documentation files (the "Software"), to deal
                        in the Software without restriction, including without limitation the rights
                        to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
                        copies of the Software.
                        ""\"
                        
                        def add_numbers(a, b):
                            return a + b
                        
                        ```
                        
                        **Expected Output:**
                        
                        ```yaml
                        ---
                        license: |
                          Permission is hereby granted, free of charge, to any person obtaining a copy
                          of this software and associated documentation files (the "Software"), to deal
                          in the Software without restriction, including without limitation the rights
                          to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
                          copies of the Software.
                        holders: |
                          Jane Doe, John Smith
                        years: |
                          2024
                        ---
                        
                        ```
                        
                        ---
                        
                        ### Example 2: No License Found
                        
                        **User Prompt:**
                        
                        ```javascript
                        // This function calculates the factorial of a given number
                        function factorial(n) {
                            if (n === 0 || n === 1) return 1;
                            return n * factorial(n - 1);
                        }
                        const result = factorial(5);
                        console.log(result);
                        
                        ```
                        
                        **Expected Output:**
                        
                        ```
                        NO
                        
                        ```
                        
                        ---
                        
                        ### Example 3: BSD License Found (Inline Comments)
                        
                        **User Prompt:**
                        
                        ```c
                        // Copyright (c) 2021-2023 Acme Corporation. All rights reserved.
                        //\s
                        // Redistribution and use in source and binary forms, with or without
                        // modification, are permitted provided that the following conditions are met:
                        // 1. Redistributions of source code must retain the above copyright notice.
                        // 2. Redistributions in binary form must reproduce the above copyright notice.
                        
                        #include <stdio.h>
                        int main() {
                            printf("Hello, World!");
                            return 0;
                        }
                        
                        ```
                        
                        **Expected Output:**
                        
                        ```yaml
                        ---
                        license: |
                          Redistribution and use in source and binary forms, with or without
                          modification, are permitted provided that the following conditions are met:
                          1. Redistributions of source code must retain the above copyright notice.
                          2. Redistributions in binary form must reproduce the above copyright notice.
                        holders: |
                          Acme Corporation
                        years: |
                          2021-2023
                        ---
                        
                        ```                        
                        Text: 
                        
                """)
                .defaultOptions(OpenAiChatOptions.builder().maxTokens(65536).build())
                //.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
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

        Map<String, LicensedFiles> licenseMap = new LinkedHashMap<>();
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
                licenseMap.get(exitingLicense).files().add(filePath.toString());
                continue;
            }

            LicenseInfo licenseInfo = extractLicense(content);
            String licenseText = licenseInfo.text();

            if (licenseText == null || licenseText.isBlank()) {
                noLicenseFiles.add(filePath.toString());
            } else {
                String normalizedKey = normalizeLicense(licenseText);
                licenseMap.computeIfAbsent(normalizedKey, k -> new LicensedFiles(licenseInfo, new ArrayList<>())).files().add(filePath.toString());
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("License scan results for: ").append(path).append("\n");
        summary.append("Files scanned: ").append(total).append("\n");
        summary.append("Distinct licenses: ").append(licenseMap.size()).append("\n");
        summary.append("Files without license: ").append(noLicenseFiles.size()).append("\n\n");

        List<String> fullLicenseFiles = new ArrayList<>();
        int licenseIdx = 1;
        for (Map.Entry<String, LicensedFiles> entry : licenseMap.entrySet()) {
            LicensedFiles lf = entry.getValue();
            summary.append("--- License #").append(licenseIdx++).append(" (").append(lf.files().size()).append(" files) ---\n");
            summary.append(lf.info().text()).append("\n");
            if (lf.info().holders() != null && !lf.info().holders().isBlank()) {
                summary.append("Holders: ").append(lf.info().holders()).append("\n");
            }
            if (lf.info().years() != null && !lf.info().years().isBlank()) {
                summary.append("Years: ").append(lf.info().years()).append("\n");
            }
            summary.append("Files:\n");
            for (String f : lf.files()) {
                summary.append("  ").append(f).append("\n");
            }
            summary.append("\n");
            if (lf.info().full()) {
                fullLicenseFiles.addAll(lf.files());
            }
        }

        if (!fullLicenseFiles.isEmpty()) {
            summary.append("--- Full License Files (").append(fullLicenseFiles.size()).append(" files) ---\n");
            for (String f : fullLicenseFiles) {
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

    private String isExistingLicense(String content, Map<String, LicensedFiles> licenseMap) {
        String normalizedContent = normalizeLicense(content);
        for (String key : licenseMap.keySet()) {
            if (normalizedContent.contains(key)) {
                return key;
            }
        }
        return null;
    }

    private LicenseInfo extractLicense(String fileContent) {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> comments = extractor.extractComments(fileContent);
        if (comments.isEmpty()) {
            sb.append(fileContent.substring(0, MAX_LICENSE_LENGTH));
        } else {
            for (var c : comments) {
                sb.append(c);
                sb.append("\n");
            }
        }
           Prompt p =
                new Prompt(sb.toString());
        var chatResponse = chatClient.prompt(p)
                .call().chatResponse();
        var license = chatResponse.getResult().getOutput().getText();
        if (license == null || "NO".equals(license) || "\nNO\n".equals(license)) {
            return new LicenseInfo(null, null, null, false);
        }
        license = license.replace("```yaml", "")
                .replace("```", "")
                .replace("---", "");
        try {
            Map<String, String> ret = new Yaml().load(license);
            var lic = ret.get("license");
            String licenseText = ret.get("license");
            String holderText = ret.get("holders");
            var yearsText = ret.get("years");
            boolean full = fileContent.length() - 10 < licenseText.length() + holderText.length() + yearsText.length();
            return new LicenseInfo(licenseText, holderText, yearsText, full);
        }
        catch (Exception e ){
            return new LicenseInfo(null, null, null, false);
        }
    }
}
