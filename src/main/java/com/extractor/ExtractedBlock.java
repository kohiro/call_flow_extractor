package com.extractor;

public class ExtractedBlock {
    private final String filePath;
    private final String content;

    public ExtractedBlock(String filePath, String content) {
        this.filePath = filePath;
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getContent() {
        return content;
    }

    public String toMarkdown() {
        return "### " + filePath + "\n\n```java\n" + content + "\n```\n";
    }
}
