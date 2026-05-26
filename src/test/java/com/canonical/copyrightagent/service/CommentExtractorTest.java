package com.canonical.copyrightagent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CommentExtractorTest {

    private CommentExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new CommentExtractorImpl();
    }

    @Test
    void singleHashComment() {
        ArrayList<String> comments = extractor.extractComments("# this is a comment\n");
        assertEquals(1, comments.size());
        assertEquals("# this is a comment", comments.get(0));
    }

    @Test
    void singleSlashComment() {
        ArrayList<String> comments = extractor.extractComments("// this is a comment\n");
        assertEquals(1, comments.size());
        assertEquals("// this is a comment", comments.get(0));
    }

    @Test
    void consecutiveHashCommentsMerged() {
        ArrayList<String> comments = extractor.extractComments("# line one\n# line two\n# line three\n");
        assertEquals(1, comments.size());
        assertEquals("# line one\n# line two\n# line three", comments.get(0));
    }

    @Test
    void consecutiveSlashCommentsMerged() {
        ArrayList<String> comments = extractor.extractComments("// line one\n// line two\n");
        assertEquals(1, comments.size());
        assertEquals("// line one\n// line two", comments.get(0));
    }

    @Test
    void nonConsecutiveSingleLineCommentsSeparate() {
        ArrayList<String> comments = extractor.extractComments("# comment one\n\n# comment two\n");
        assertEquals(2, comments.size());
        assertEquals("# comment one", comments.get(0));
        assertEquals("# comment two", comments.get(1));
    }

    @Test
    void singleLineCommentsSeparatedByCodeAreSeparate() {
        ArrayList<String> comments = extractor.extractComments("# first\nint x = 1;\n# second\n");
        assertEquals(2, comments.size());
        assertEquals("# first", comments.get(0));
        assertEquals("# second", comments.get(1));
    }

    @Test
    void blockComment() {
        ArrayList<String> comments = extractor.extractComments("/* this is\na block comment */");
        assertEquals(1, comments.size());
        assertEquals("/* this is\na block comment */", comments.get(0));
    }

    @Test
    void docstring() {
        ArrayList<String> comments = extractor.extractComments("\"\"\"this is\na docstring\"\"\"");
        assertEquals(1, comments.size());
        assertEquals("\"\"\"this is\na docstring\"\"\"", comments.get(0));
    }

    @Test
    void leadingWhitespaceStrippedFromSingleLine() {
        ArrayList<String> comments = extractor.extractComments("    # indented comment\n");
        assertEquals(1, comments.size());
        assertEquals("# indented comment", comments.get(0));
    }

    @Test
    void mixedComments() {
        String content = "# header\n/* block */\n// single\n\"\"\"doc\"\"\"";
        ArrayList<String> comments = extractor.extractComments(content);
        assertEquals(4, comments.size());
        assertEquals("# header", comments.get(0));
        assertEquals("/* block */", comments.get(1));
        assertEquals("// single", comments.get(2));
        assertEquals("\"\"\"doc\"\"\"", comments.get(3));
    }

    @Test
    void noComments() {
        ArrayList<String> comments = extractor.extractComments("int x = 5;\nString s = \"hello\";");
        assertTrue(comments.isEmpty());
    }

    @Test
    void xmlComment() {
        ArrayList<String> comments = extractor.extractComments("<!-- xml comment -->");
        assertEquals(1, comments.size());
        assertEquals("<!-- xml comment -->", comments.get(0));
    }

    @Test
    void multilineXmlComment() {
        ArrayList<String> comments = extractor.extractComments("<!-- line one\nline two -->");
        assertEquals(1, comments.size());
        assertEquals("<!-- line one\nline two -->", comments.get(0));
    }

    @Test
    void emptyContent() {
        ArrayList<String> comments = extractor.extractComments("");
        assertTrue(comments.isEmpty());
    }
}
