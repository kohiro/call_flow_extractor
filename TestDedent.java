public class TestDedent {
    public static void main(String[] args) {
        String fileContent = "public class OrderRecord {\n" +
                             "// --- フィールド ---\n" +
                             "private ExchangeService service;\n" +
                             "\n" +
                             "// --- メソッド定義 ---\n" +
                             "@JsonIgnore\n" +
                             "\tpublic boolean isActive() {\n" +
                             "\t\treturn getStatus() == null || !getStatus().isFinal();\n" +
                             "\t}\n" +
                             "}\n";

        // Let's pretend start is at '@' of @JsonIgnore
        int start = fileContent.indexOf("@JsonIgnore");
        int length = "@JsonIgnore\n\tpublic boolean isActive() {\n\t\treturn getStatus() == null || !getStatus().isFinal();\n\t}".length();

        System.out.println(extractSourceDedented(fileContent, start, length));
    }

    private static String extractSourceDedented(String fileContent, int start, int length) {
        // Go back to the very beginning of the line
        int lineStart = start;
        while (lineStart > 0 && fileContent.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        
        String rawSource = fileContent.substring(lineStart, start + length);
        String[] lines = rawSource.split("\n", -1);
        
        String firstLinePrefix = fileContent.substring(lineStart, start);
        boolean hasNonWhitespacePrefix = !firstLinePrefix.trim().isEmpty();
        
        int minIndentLength = Integer.MAX_VALUE;
        String minIndent = "";
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // Remove \r if present
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
                lines[i] = line;
            }
            
            if (i == 0 && hasNonWhitespacePrefix) {
                continue;
            }
            if (line.trim().isEmpty()) {
                continue;
            }
            
            String currentIndent = "";
            for (int j = 0; j < line.length(); j++) {
                char c = line.charAt(j);
                if (c == ' ' || c == '\t') {
                    currentIndent += c;
                } else {
                    break;
                }
            }
            if (currentIndent.length() < minIndentLength) {
                minIndentLength = currentIndent.length();
                minIndent = currentIndent;
            }
        }
        
        System.out.println("minIndentLength: " + minIndentLength);
        System.out.println("minIndent: [" + minIndent + "]");
        
        if (minIndentLength == Integer.MAX_VALUE || minIndentLength == 0) {
            return fileContent.substring(start, start + length);
        }
        
        StringBuilder dedented = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            if (i == 0 && hasNonWhitespacePrefix) {
                String nodeContentOnFirstLine = line.substring(firstLinePrefix.length());
                dedented.append(nodeContentOnFirstLine);
            } else {
                if (line.startsWith(minIndent)) {
                    dedented.append(line.substring(minIndent.length()));
                } else if (line.trim().isEmpty()) {
                    dedented.append("");
                } else {
                    dedented.append(line);
                }
            }
            
            if (i < lines.length - 1) {
                dedented.append("\n");
            }
        }
        return dedented.toString();
    }
}
