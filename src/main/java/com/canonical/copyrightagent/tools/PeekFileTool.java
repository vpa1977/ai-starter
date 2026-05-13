package com.canonical.copyrightagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PeekFileTool {

    private final int HEADER = 8192;

    @Tool(name = "peek", description = "Read the first 8KB header from the file at the given path")
    public String peek(@ToolParam(description = "path to read")  String path) {
        try {
            String ret = Files.readString(Path.of(path));
            if (ret.length() > HEADER) {
                return ret.substring(0, HEADER);
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Tool(name = "read", description = "Read file from the path")
    public String read(@ToolParam(description = "path to read") String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

}
