package com.canonical.copyrightagent.service;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class CommentExtractorImpl implements CommentExtractor {

    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "\"\"\"[\\s\\S]*?\"\"\"|<!--[\\s\\S]*?-->|/\\*[\\s\\S]*?\\*/|[ \\t]*(?:#|//).*");

    @Override
    public ArrayList<String> extractComments(String content) {
        ArrayList<String> comments = new ArrayList<>();
        Matcher matcher = COMMENT_PATTERN.matcher(content);

        StringBuilder pendingSingleLine = null;
        int lastSingleLineEnd = -1;

        while (matcher.find()) {
            String match = matcher.group().stripLeading();
            boolean isSingleLine = match.startsWith("#") || match.startsWith("//");

            if (isSingleLine) {
                if (pendingSingleLine != null) {
                    String between = content.substring(lastSingleLineEnd, matcher.start());
                    if (between.matches("\\r?\\n")) {
                        pendingSingleLine.append("\n").append(match);
                    } else {
                        comments.add(pendingSingleLine.toString());
                        pendingSingleLine = new StringBuilder(match);
                    }
                } else {
                    pendingSingleLine = new StringBuilder(match);
                }
                lastSingleLineEnd = matcher.end();
            } else {
                if (pendingSingleLine != null) {
                    comments.add(pendingSingleLine.toString());
                    pendingSingleLine = null;
                    lastSingleLineEnd = -1;
                }
                comments.add(match);
            }
        }

        if (pendingSingleLine != null) {
            comments.add(pendingSingleLine.toString());
        }

        return comments;
    }
}
