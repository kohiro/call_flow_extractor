package com.extractor;

import java.io.IOException;
import java.util.List;

public class CallFlowExtractorMain {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar call-flow-extractor.jar <ProjectRootPath> [OutputDirectory]");
            System.exit(1);
        }

        String projectRoot = args[0];
        String outputDir = args.length > 1 ? args[1] : ".";

        try {
            ProjectIndexer indexer = new ProjectIndexer();
            indexer.indexProject(projectRoot);

            List<ProjectIndexer.EntryMethod> entryMethods = indexer.getEntryMethods();
            if (entryMethods.isEmpty()) {
                System.out.println("No @RequestMapping methods found.");
                return;
            }

            java.nio.file.Path outDirPath = java.nio.file.Path.of(outputDir);
            if (!java.nio.file.Files.exists(outDirPath)) {
                java.nio.file.Files.createDirectories(outDirPath);
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
                java.nio.file.Path outPath = outDirPath.resolve(filename);
                java.nio.file.Files.writeString(outPath, markdownOutput.toString());
                System.out.println("Generated: " + outPath);
            }

        } catch (IOException e) {
            System.err.println("Error indexing project: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
