import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class ClassExtractor {
    private static int classCount = 0; // Counter for classes

    public static void main(String[] args) {
        String projectDir = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Projects/cucumber-jvm"; // Update this path

        try {
            Files.walk(Paths.get(projectDir))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(ClassExtractor::parse);

            // Print the total number of classes after processing all files
            System.out.println("Total number of classes: " + classCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parse(Path path) {
        try {
            // Parse the Java file
            ParseResult<CompilationUnit> parseResult = new JavaParser().parse(path);

            // Check if the result contains a CompilationUnit
            Optional<CompilationUnit> cu = parseResult.getResult();

            // If present, visit the CompilationUnit
            if (cu.isPresent()) {
                VoidVisitor<Path> classVisitor = new ClassSignaturePrinter();
                classVisitor.visit(cu.get(), path);
            } else {
                System.err.println("Failed to parse file: " + path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClassSignaturePrinter extends VoidVisitorAdapter<Path> {
        @Override
        public void visit(ClassOrInterfaceDeclaration cid, Path path) {
            super.visit(cid, path);
            System.out.println("File Path: " + path.toString());
            System.out.println("Class Name: " + cid.getName());
            System.out.println();

            // Increment the class counter
            classCount++;
        }
    }
}
  