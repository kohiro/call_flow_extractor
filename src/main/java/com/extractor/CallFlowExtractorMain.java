package com.extractor;

import java.io.IOException;
import java.util.List;

public class CallFlowExtractorMain {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java -jar call-flow-extractor.jar <ProjectRootPath> <ControllerFQCN> <MethodName>");
            System.exit(1);
        }

        String projectRoot = args[0];
        String controllerFqcn = args[1];
        String methodName = args[2];

        try {
            ProjectIndexer indexer = new ProjectIndexer();
            indexer.indexProject(projectRoot);

            FlowTracer tracer = new FlowTracer(indexer);
            List<ExtractedBlock> blocks = tracer.trace(controllerFqcn, methodName, -1);

            StringBuilder markdownOutput = new StringBuilder();
            markdownOutput.append("# Data Flow Extraction\n\n");
            markdownOutput.append("**Start:** `").append(controllerFqcn).append("#").append(methodName).append("`\n\n");

            for (ExtractedBlock block : blocks) {
                markdownOutput.append(block.toMarkdown());
            }

            System.out.println(markdownOutput.toString());

        } catch (IOException e) {
            System.err.println("Error indexing project: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
