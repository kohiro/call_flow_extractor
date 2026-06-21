package com.extractor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

            JsonObject payload = new JsonObject();
            payload.addProperty("model", modelName);
            
            JsonArray messages = new JsonArray();
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messages.add(sysMsg);

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", markdownContent);
            messages.add(userMsg);

            payload.add("messages", messages);
            payload.addProperty("stream", false);

            JsonObject options = new JsonObject();
            options.addProperty("num_ctx", numCtx);
            payload.add("options", options);

            Gson gson = new Gson();
            String jsonBody = gson.toJson(payload);

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
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                String content = "";
                if (jsonResponse.has("message") && jsonResponse.getAsJsonObject("message").has("content")) {
                    content = jsonResponse.getAsJsonObject("message").get("content").getAsString();
                } else if (jsonResponse.has("response")) {
                    // fallback for older Ollama /api/generate endpoints if user configures that
                    content = jsonResponse.get("response").getAsString();
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
