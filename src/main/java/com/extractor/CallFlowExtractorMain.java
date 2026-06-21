package com.extractor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class CallFlowExtractorMain {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java -jar call-flow-extractor.jar <ProjectRootPath> <OutputDirectory> [ApiEndpoint] [SystemPromptMd] [ApiResultOutputDir] [ModelName] [NumCtx]");
            System.exit(1);
        }

        String projectRoot = args[0];
        String outputDir = args[1];

        String apiEndpoint = null;
        String systemPromptMd = null;
        String apiResultDir = null;
        String modelName = "gemma4:31b";
        int numCtx = 256000;

        if (args.length >= 5) {
            apiEndpoint = args[2];
            systemPromptMd = args[3];
            apiResultDir = args[4];
        }
        if (args.length >= 6) {
            modelName = args[5];
        }
        if (args.length >= 7) {
            try {
                numCtx = Integer.parseInt(args[6]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid numCtx value, using default: " + numCtx);
            }
        }

        List<String> extraFilesList = new ArrayList<>();
        Path yamlConfig = Path.of("extra-files.yaml");
        if (Files.exists(yamlConfig)) {
            try {
                ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
                JsonNode root = yamlMapper.readTree(yamlConfig.toFile());
                if (root != null && root.has("extraFiles")) {
                    JsonNode extraFilesNode = root.get("extraFiles");
                    if (extraFilesNode.isArray()) {
                        for (JsonNode node : extraFilesNode) {
                            extraFilesList.add(node.asText());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Warning: Failed to read extra-files.yaml: " + e.getMessage());
            }
        }

        try {
            ProjectIndexer indexer = new ProjectIndexer();
            indexer.indexProject(projectRoot);

            List<ProjectIndexer.EntryMethod> entryMethods = indexer.getEntryMethods();
            if (entryMethods.isEmpty()) {
                System.out.println("No @RequestMapping methods found.");
                return;
            }

            Path outDirPath = Path.of(outputDir);
            if (!Files.exists(outDirPath)) {
                Files.createDirectories(outDirPath);
            }

            for (ProjectIndexer.EntryMethod entry : entryMethods) {
                FlowTracer tracer = new FlowTracer(indexer, outDirPath);
                List<ExtractedBlock> blocks = tracer.trace(entry.fqcn, entry.methodName, -1);

                if (blocks.isEmpty()) {
                    continue;
                }

                StringBuilder markdownOutput = new StringBuilder();
                markdownOutput.append("# Data Flow Extraction\n\n");
                markdownOutput.append("**Start:** `").append(entry.fqcn).append("#").append(entry.methodName).append("`\n\n");

                for (ExtractedBlock block : blocks) {
                    markdownOutput.append(block.toMarkdown());
                }

                if (!extraFilesList.isEmpty()) {
                    markdownOutput.append("\n## 追加ファイル\n\n");
                    for (String filePath : extraFilesList) {
                        Path extraPath = Path.of(filePath);
                        if (Files.exists(extraPath)) {
                            markdownOutput.append("### ").append(extraPath.getFileName().toString()).append("\n\n");
                            String ext = "";
                            String filenameExt = extraPath.getFileName().toString();
                            int dotIndex = filenameExt.lastIndexOf('.');
                            if (dotIndex > 0) {
                                ext = filenameExt.substring(dotIndex + 1);
                            }
                            markdownOutput.append("```").append(ext).append("\n");
                            try {
                                markdownOutput.append(Files.readString(extraPath));
                            } catch (IOException e) {
                                markdownOutput.append("Error reading file: ").append(e.getMessage());
                            }
                            markdownOutput.append("\n```\n\n");
                        } else {
                            System.err.println("Warning: Extra file not found: " + filePath);
                        }
                    }
                }

                String filename = entry.fqcn + "_" + entry.methodName + ".md";
                Path outPath = outDirPath.resolve(filename);
                Files.writeString(outPath, markdownOutput.toString());
                System.out.println("Generated: " + outPath);

                if (apiEndpoint != null) {
                    sendToOllamaApi(apiEndpoint, systemPromptMd, apiResultDir, modelName, numCtx, markdownOutput.toString(), filename);
                }
            }

        } catch (IOException e) {
            System.err.println("Error indexing project: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendToOllamaApi(String apiEndpoint, String systemPromptPath, String apiResultOutputDir, String modelName, int numCtx, String markdownContent, String filename) {
        try {
            String systemPrompt = Files.readString(Path.of(systemPromptPath));

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();
            payload.put("model", modelName);
            
            ArrayNode messages = mapper.createArrayNode();
            ObjectNode sysMsg = mapper.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);

            ObjectNode userMsg = mapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", markdownContent);
            messages.add(userMsg);

            payload.set("messages", messages);
            payload.put("stream", false);

            ObjectNode options = mapper.createObjectNode();
            options.put("num_ctx", numCtx);
            payload.set("options", options);

            String jsonBody = mapper.writeValueAsString(payload);

            HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiEndpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            System.out.println("Sending to Ollama API: " + filename + " using model: " + modelName);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectNode jsonResponse = (ObjectNode) mapper.readTree(response.body());
                String content = "";
                if (jsonResponse.has("message") && jsonResponse.get("message").has("content")) {
                    content = jsonResponse.get("message").get("content").asText();
                } else if (jsonResponse.has("response")) {
                    // fallback for older Ollama /api/generate endpoints if user configures that
                    content = jsonResponse.get("response").asText();
                } else {
                    content = response.body();
                }

                Path outDirPath = Path.of(apiResultOutputDir);
                if (!Files.exists(outDirPath)) {
                    Files.createDirectories(outDirPath);
                }
                Path outFilePath = outDirPath.resolve(filename);
                Files.writeString(outFilePath, content);
                System.out.println("API response saved to: " + outFilePath);
            } else {
                System.err.println("API error for " + filename + ": HTTP " + response.statusCode());
                System.err.println(response.body());
            }

        } catch (Exception e) {
            System.err.println("Failed to send API request for " + filename);
            e.printStackTrace();
        }
    }
}
