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

public class UnmockedMethodsUnmocked {
    public static void main(String[] args) {
        String csvFilePath = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Final/Unmocked_Classes.csv";
        String outputCsvFilePath = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Final/UnmockedMethodUnmocked.csv";
        String line;
        String csvSplitBy = ",";
        int totalMethods = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath));
             FileWriter writer = new FileWriter(outputCsvFilePath)) {

            // Write header
            writer.write("Method Signature,Container Class Signature,Start Line,End Line,Fully Qualified Name of Container Class,Class Path,Method Label\n");

            // Skip header in input
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] columns = line.split(csvSplitBy);
                if (columns.length > 1) {
                    String classPath = columns[1].trim();
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

            System.out.println("Total number of methods: " + totalMethods);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int extractMethodSignatures(File classFile, FileWriter writer) {
        try {
            JavaParser parser = new JavaParser();
            ParseResult<CompilationUnit> result = parser.parse(classFile);

            if (result.isSuccessful() && result.getResult().isPresent()) {
                CompilationUnit cu = result.getResult().get();
                MethodVisitor visitor = new MethodVisitor(classFile.getPath(), writer);
                cu.accept(visitor, null);
                return visitor.getMethodCount();
            } else {
                System.out.println("Failed to parse the class file: " + classFile.getPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static class MethodVisitor extends VoidVisitorAdapter<Void> {
        private final String filePath;
        private final FileWriter writer;
        private String className;
        private String classSignature;
        private int methodCount = 0;

        public MethodVisitor(String filePath, FileWriter writer) {
            this.filePath = filePath;
            this.writer = writer;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration cid, Void arg) {
            className = cid.getFullyQualifiedName().orElse("[Unknown Class]");

            String modifiers = cid.getModifiers().stream()
                    .map(modifier -> modifier.getKeyword().asString())
                    .collect(Collectors.joining(" "));

            String typeParameters = cid.getTypeParameters().isEmpty() ? "" : cid.getTypeParameters().toString();

            classSignature = modifiers + " " + cid.getNameAsString() + typeParameters;
            classSignature = classSignature.trim();

            super.visit(cid, arg);
        }

        @Override
        public void visit(MethodDeclaration md, Void arg) {
            super.visit(md, arg);
            methodCount++;

            int startLine = md.getBegin().isPresent() ? md.getBegin().get().line : -1;
            if (!md.getAnnotations().isEmpty()) {
                startLine += md.getAnnotations().size();
            }

            int endLine = md.getEnd().isPresent() ? md.getEnd().get().line : -1;
            String methodSignature = md.getDeclarationAsString();

            System.out.println(String.format("Method: %s | Class: %s | Start Line: %d | End Line: %d | File: %s",
                    methodSignature, classSignature, startLine, endLine, filePath));

            try {
                writer.write(String.format("%s,%s,%d,%d,%s,%s,Unmocked\n",
                        escapeCsv(methodSignature),
                        escapeCsv(classSignature),
                        startLine,
                        endLine,
                        escapeCsv(className),
                        escapeCsv(filePath)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public int getMethodCount() {
            return methodCount;
        }
    }

    // Escapes fields for safe CSV writing
    private static String escapeCsv(String value) {
        if (value == null) return "";

        boolean hasSpecial = value.contains(",") || value.contains("\"") || value.contains("\n");
        value = value.replace("\"", "\"\""); // escape double quotes

        return hasSpecial ? "\"" + value + "\"" : value;
    }
}
