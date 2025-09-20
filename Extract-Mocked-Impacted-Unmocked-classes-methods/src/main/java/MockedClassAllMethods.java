import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

public class MockedClassAllMethods {
    public static void main(String[] args) {
        String csvFilePath = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Final/Mocked_classes.csv";
        String outputCsvFilePath = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/MockedClassAllMethods.csv";
        String line;
        String csvSplitBy = ",";
        int totalMethods = 0; // Initialize a counter for the total number of methods

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath));
             FileWriter writer = new FileWriter(outputCsvFilePath)) {

            // Write the header to the output CSV file
            writer.write("Method Signature,Container Class Signature,Start Line,End Line,Fully Qualified Name of Container Class,Class Path\n");

            // Skip the header line if your CSV has one
            br.readLine();

            while ((line = br.readLine()) != null) {
                // Assuming the class name is in the first column and class path is in the second column
                String[] columns = line.split(csvSplitBy);
                if (columns.length > 1) {
                    String classPath = columns[1].trim();

                    // Convert the class path to a file path
                    File classFile = new File(classPath);
                    if (classFile.exists()) {
                        totalMethods += extractMethodSignatures(classFile, writer);
                    } else {
                        System.out.println("Class file not found: " + classPath);
                    }
                } else {
                    System.out.println("Invalid line: " + line);
                }
            }

            // Print the total number of methods
            System.out.println("Total number of methods: " + totalMethods);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int extractMethodSignatures(File classFile, FileWriter writer) {
        try {
            // Create an instance of JavaParser
            JavaParser parser = new JavaParser();
            ParseResult<CompilationUnit> result = parser.parse(classFile);

            // Check if parsing was successful and get the CompilationUnit
            if (result.isSuccessful() && result.getResult().isPresent()) {
                CompilationUnit cu = result.getResult().get();
                MethodVisitor visitor = new MethodVisitor(classFile.getPath(), writer);
                cu.accept(visitor, null);
                return visitor.getMethodCount(); // Return the number of methods found
            } else {
                System.out.println("Failed to parse the class file: " + classFile.getPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0; // Return 0 if no methods were found or parsing failed
    }

    private static class MethodVisitor extends VoidVisitorAdapter<Void> {
        private final String filePath;
        private final FileWriter writer;
        private String className;
        private String classSignature;
        private int methodCount = 0; // Counter for the number of methods

        public MethodVisitor(String filePath, FileWriter writer) {
            this.filePath = filePath;
            this.writer = writer;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
            // Capture the fully qualified name of the container class
            className = cid.getFullyQualifiedName().orElse("[Unknown Class]");
            
            // Extract class modifiers (public, abstract, etc.)
            String modifiers = cid.getModifiers().stream()
                    .map(modifier -> modifier.getKeyword().asString())
                    .collect(Collectors.joining(" "));
            
            // Extract class name and type parameters (e.g., <T, U>)
            String typeParameters = cid.getTypeParameters().isEmpty() ? "" : cid.getTypeParameters().toString();
            
            // Construct the class signature with modifiers and parameters
            classSignature = modifiers + " " + cid.getNameAsString() + typeParameters;
            classSignature = classSignature.trim(); // Clean up extra spaces

            super.visit(cid, arg);
        }

        @Override
        public void visit(MethodDeclaration md, Void arg) {
            super.visit(md, arg);
            methodCount++; // Increment the method counter

            // Extract method details
            String methodSignature = md.getDeclarationAsString();
            int startLine = md.getBegin().isPresent() ? md.getBegin().get().line : -1;
            int endLine = md.getEnd().isPresent() ? md.getEnd().get().line : -1;

            // Adjust the start line if there are annotations
            if (md.getAnnotations().isNonEmpty()) {
                startLine += md.getAnnotations().size(); // Adjust start line number based on the number of annotations
            }

            // Print the method information to the console
            System.out.println(String.format("Method: %s | Class: %s | Start Line: %d | End Line: %d | File: %s",
                    methodSignature, classSignature, startLine, endLine, filePath));

            // Write the method information to the CSV file
            try {
                writer.write(String.format("\"%s\",\"%s\",%d,%d,\"%s\",\"%s\"\n",
                        methodSignature, classSignature, startLine, endLine, className, filePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public int getMethodCount() {
            return methodCount;
        }
    }
}