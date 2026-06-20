package com.extractor;

public class ExtractedBlock {
    private final String title;
    private final String content;
    private final String anchorId;

    public ExtractedBlock(String title, String content, String anchorId) {
        this.title = title;
        this.content = content;
        this.anchorId = anchorId;
    }

    public ExtractedBlock(String title, String content) {
        this(title, content, null);
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String toMarkdown() {
        if (anchorId != null) {
            return "<a id=\"" + anchorId + "\"></a>\n### " + title + "\n\n```java\n" + content + "\n```\n";
        }
        return "### " + title + "\n\n```java\n" + content + "\n```\n";
    }
}
