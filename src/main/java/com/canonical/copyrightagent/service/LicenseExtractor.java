package com.canonical.copyrightagent.service;

import com.canonical.copyrightagent.model.LicenseInfo;
import org.debian.decopy.matchers.LicenseMatcher;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;

@Component
public class LicenseExtractor {
    private final ChatClient chatClient;

    public LicenseExtractor(ModelService service, @Value("classpath:/system-prompt.md") Resource systemPrompt) throws IOException {
        var sysPromptString = systemPrompt.getContentAsString(Charset.defaultCharset());
        //var model = service.createModel("liquid/lfm-2-24b-a2b");
        //var model = service.createModel("meta-llama/llama-3.1-8b-instruct");
        var model = service.createModel("qwen/qwen3.5-9b");
        //var model = service.createModel("nvidia/nemotron-3-nano-4b");
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();

        chatClient = ChatClient.builder(model)
                .defaultSystem(sysPromptString)
                .defaultOptions(OpenAiChatOptions.builder().maxTokens(2048).build())
                //.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * Returns first found license in the comment block
     * @param licenseCommentData
     * @param length
     * @return
     */
    public LicenseInfo extractLicense(String licenseCommentData, long length) {
        var foundLicenses = LicenseMatcher.findLicenses(licenseCommentData);
        for (var licenseData  : foundLicenses.keySet()) {
            Prompt p =
                    new Prompt(licenseData);
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
                String licenseText = ret.get("spdx");
                String holderText = ret.get("copyright");
                var yearsText = ret.get("years");
                boolean full = length - 10 < licenseText.length() + holderText.length() + yearsText.length();
                return new LicenseInfo(licenseText, holderText, yearsText, full);
            }
            catch (Exception e ){
                return new LicenseInfo(null, null, null, false);
            }
        }
        return new LicenseInfo(null, null, null, false);
    }
}
