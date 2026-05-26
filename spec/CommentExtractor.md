# Comment extractor

Write regular expression parser that extracts comments from the file.

# Comment format

Single line:  line starting with # and //
Comment block: characters within /* and */
Docstring: characters within """ and """
Xml comment: characters within <!-- -->

# Merge single line comments

consecutive single line comments is extracted as single comment

# API

package com.canonical.copyrightagent.service;

public interface CommentExtractor {
    public ArrayList<String> extractComments(String content);
}

# Tests

Add tests for single line, comment block, docstring, xml comment