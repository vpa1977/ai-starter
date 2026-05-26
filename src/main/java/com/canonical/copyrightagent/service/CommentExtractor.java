package com.canonical.copyrightagent.service;

import java.util.ArrayList;

public interface CommentExtractor {
    ArrayList<String> extractComments(String content);
}
