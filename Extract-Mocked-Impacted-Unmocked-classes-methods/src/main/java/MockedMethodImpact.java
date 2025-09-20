import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class MockedMethodImpact {

    private static final AtomicInteger methodCount = new AtomicInteger(0);
    private static final Map<String, String> MOCK_METHODS = new HashMap<>();

    static {
        MOCK_METHODS.put("when", "org.mockito.Mockito.when");
        MOCK_METHODS.put("given", "org.mockito.BDDMockito.given");
        MOCK_METHODS.put("defaultAnswer", "org.mockito.Mockito.defaultAnswer");
        MOCK_METHODS.put("lenient", "org.mockito.Mockito.lenient");
        MOCK_METHODS.put("mockStatic", "org.mockito.Mockito.mockStatic");
    }

    public static void main(String[] args) {
        String projectDir = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Projects/cucumber-jvm";  // Update this path to your project
        String csvFilePath = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Final/ImpactedByMocking.csv";  // Output CSV file path

        try (PrintWriter writer = new PrintWriter(csvFilePath)) {
            writer.println("Method Signature,Container Class Signature,Start Line,End Line,Fully Qualified Name of Container Class,Class Path,Method Label");

            Files.walk(Paths.get(projectDir))
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> parse(path, writer));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Total number of mocking methods extracted: " + methodCount.get());
    }

    private static void parse(Path path, PrintWriter writer) {
        try {
            ParseResult<CompilationUnit> parseResult = new JavaParser().parse(path);
            Optional<CompilationUnit> cu = parseResult.getResult();

            if (cu.isPresent()) {
                CompilationUnit compilationUnit = cu.get();
                VoidVisitorAdapter<Path> visitor = new MethodExtractor(writer, compilationUnit);
                visitor.visit(compilationUnit, path);
            } else {
                System.err.println("Failed to parse file: " + path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class MethodExtractor extends VoidVisitorAdapter<Path> {

        private final PrintWriter writer;
        private final CompilationUnit cu;

        public MethodExtractor(PrintWriter writer, CompilationUnit cu) {
            this.writer = writer;
            this.cu = cu;
        }

        @Override
        public void visit(MethodDeclaration md, Path path) {
            super.visit(md, path);

            // Check for mocking usage
            boolean isMockingMethod = md.findAll(MethodCallExpr.class).stream()
                    .anyMatch(call -> MOCK_METHODS.containsKey(call.getNameAsString()));

            if (isMockingMethod) {
                // Container class
                Optional<ClassOrInterfaceDeclaration> parentClassOpt = md.findAncestor(ClassOrInterfaceDeclaration.class);
                if (parentClassOpt.isEmpty()) return;
                ClassOrInterfaceDeclaration parentClass = parentClassOpt.get();

                // Extract fully qualified class name (with package)
                String packageName = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
                String className = parentClass.getNameAsString();
                String fullyQualifiedName = packageName.isEmpty() ? className : packageName + "." + className;

                // Method signature
                String accessModifiers = md.getModifiers().toString().replaceAll("[\\[\\]]", "");
                String returnType = md.getTypeAsString();
                String parameters = md.getParameters().toString().replaceAll("[\\[\\]]", "");
                String methodSignature = String.format("%s %s %s(%s)", accessModifiers, returnType, md.getNameAsString(), parameters);

                // Class signature
                String classModifiers = parentClass.getModifiers().toString().replaceAll("[\\[\\]]", "");
                String classSignature = String.format("%s class %s", classModifiers, className);

                int startLine = md.getBody()
                	    .flatMap(body -> body.getBegin())
                	    .map(pos -> pos.line)
                	    .orElse(md.getBegin().map(pos -> pos.line).orElse(-1));

                int endLine = md.getEnd().map(pos -> pos.line).orElse(-1);

                String classPath = path.toString();
                String methodLabel = "Impacted";

                // Write CSV
                String csvLine = String.format("\"%s\",\"%s\",%d,%d,\"%s\",\"%s\",\"%s\"",
                        methodSignature, classSignature, startLine, endLine,
                        fullyQualifiedName, classPath, methodLabel);

                writer.println(csvLine);
                methodCount.incrementAndGet();
            }
        }
    }
}