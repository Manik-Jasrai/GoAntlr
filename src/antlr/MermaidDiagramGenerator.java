package antlr;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

class MermaidDiagramGenerator {
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    
    public static void generateDiagramFile(String mermaidCode, String outputPath, String format) throws IOException, InterruptedException {
        // Create temp file for mermaid code
        String mermaidFile = TEMP_DIR + "/diagram.mmd";
        Files.write(Paths.get(mermaidFile), mermaidCode.getBytes());
        
        // Build the mmdc command
        ProcessBuilder pb = new ProcessBuilder(
            "mmdc",                      // Mermaid CLI command
            "-i", mermaidFile,          // Input file
            "-o", outputPath,           // Output file
            "-b", "transparent"         // Background color
        );
        
        // Add configuration for specific formats
        if (format.equals("svg")) {
            pb.command().add("-c");
            pb.command().add(createConfig());
        }
        
        // Redirect error stream to output stream
        pb.redirectErrorStream(true);
        
        // Start the process
        Process process = pb.start();
        
        // Read the output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        
        // Wait for the process to complete
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new InterruptedException("Diagram generation timed out");
        }
        
        // Check if the process completed successfully
        if (process.exitValue() != 0) {
            throw new IOException("Failed to generate diagram: Process exited with " + process.exitValue());
        }
        
        // Clean up temp file
        Files.deleteIfExists(Paths.get(mermaidFile));
    }
    
    private static String createConfig() throws IOException {
        // Create a configuration file for SVG output
        String config = "{"
            + "\"theme\": \"default\","
            + "\"flowchart\": {"
            + "  \"curve\": \"linear\","
            + "  \"padding\": 15"
            + "}"
            + "}";
        
        String configFile = TEMP_DIR + "/mermaid-config.json";
        Files.write(Paths.get(configFile), config.getBytes());
        return configFile;
    }
}