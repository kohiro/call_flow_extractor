package com.extractor;

public class ExtractedBlock {
    private final String title;
    private final String content;
    private final String anchorId;
    private final String fileLink;

    public ExtractedBlock(String title, String content, String anchorId, String fileLink) {
        this.title = title;
        this.content = content;
        this.anchorId = anchorId;
        this.fileLink = fileLink;
    }

    public ExtractedBlock(String title, String content, String anchorId) {
        this(title, content, anchorId, null);
    }

    public ExtractedBlock(String title, String content) {
        this(title, content, null, null);
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getFileLink() {
        return fileLink;
    }

    public String toMarkdown() {
        String displayTitle = (fileLink != null && !fileLink.isEmpty()) ? "[" + title + "](" + fileLink + ")" : title;
        if (anchorId != null) {
            return "<a id=\"" + anchorId + "\"></a>\n### " + displayTitle + "\n\n```java\n" + content + "\n```\n";
        }
        return "### " + displayTitle + "\n\n```java\n" + content + "\n```\n";
    }
}
