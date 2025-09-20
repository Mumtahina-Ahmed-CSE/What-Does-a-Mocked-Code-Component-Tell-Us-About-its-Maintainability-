import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.io.InputStream; // Import InputStream
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MethodInfoExtractor {
    public static void main(String[] args) {
        // Specify the project directory path here
        String projectPath = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Projects/cucumber-jvm";  // Update with your project path

        try {
            // Traverse all files in the directory and its subdirectories
            Files.walk(Paths.get(projectPath))
                .filter(Files::isRegularFile)
                .filter(file -> file.toString().endsWith(".java"))
                .forEach(MethodInfoExtractor::parseJavaFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parseJavaFile(Path filePath) {
        try (InputStream in = Files.newInputStream(filePath)) { // Use InputStream instead of var
            // Create a new JavaParser instance
            JavaParser javaParser = new JavaParser();

            // Parse the Java file
            ParseResult<CompilationUnit> parseResult = javaParser.parse(in);

            // Check if parsing was successful
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();

                // Visit all methods in the compilation unit
                cu.accept(new MethodVisitor(filePath), null);
            } else {
                System.out.printf("Parsing failed for file: %s%n", filePath);
            }
        } catch (IOException e) {
            System.err.printf("Error reading file: %s - %s%n", filePath, e.getMessage());
        }
    }

    // Visitor class to extract method information
    private static class MethodVisitor extends VoidVisitorAdapter<Void> {
        private final Path filePath;

        public MethodVisitor(Path filePath) {
            this.filePath = filePath;
        }

        @Override
        public void visit(MethodDeclaration method, Void arg) {
            super.visit(method, arg);

            // Get the starting line; adjust for annotations
            int startLine = method.getBegin().get().line;

            // Check if there are any annotations
            if (method.getAnnotations().isNonEmpty()) {
                // If there are annotations, the starting line should be after the last annotation
                startLine += method.getAnnotations().size(); // Adjust start line number
            }

            // Get the ending line
            int endLine = method.getEnd().get().line;
            // Get the method signature without annotations
            String methodSignature = method.getDeclarationAsString(true, false, false);

            // Print method information
            System.out.printf("File: %s%n", filePath);
            System.out.printf("Method Signature: %s%n", methodSignature);
            System.out.printf("Starting Line: %d%n", startLine);
            System.out.printf("Ending Line: %d%n%n", endLine);
        }
    }
}