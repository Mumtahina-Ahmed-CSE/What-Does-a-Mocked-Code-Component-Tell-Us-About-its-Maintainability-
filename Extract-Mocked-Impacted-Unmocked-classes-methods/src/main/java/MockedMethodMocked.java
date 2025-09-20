import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
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

public class MockedMethodMocked {

    private static final AtomicInteger methodCount = new AtomicInteger(0);
    private static final Map<String, String> MOCK_METHODS = new HashMap<>();
    private static final StringBuilder csvData = new StringBuilder();
    private static final Map<String, String> mockedMethods = new HashMap<>(); // Store mocked method signatures

    static {
        // Methods for mocking methods
        MOCK_METHODS.put("when", "org.mockito.Mockito.when");
        MOCK_METHODS.put("given", "org.mockito.BDDMockito.given");
        MOCK_METHODS.put("defaultAnswer", "org.mockito.Mockito.defaultAnswer");
        MOCK_METHODS.put("lenient", "org.mockito.Mockito.lenient");
        MOCK_METHODS.put("mockStatic", "org.mockito.Mockito.mockStatic"); // Add mockStatic to the map
    }

    public static void main(String[] args) {
        String projectDir = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Projects/cucumber-jvm"; // Update this path
        String csvFilePath = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Mocked_Method_Mocked.csv"; // Update CSV file path

        // Add CSV header
        csvData.append("Method Signature,Container Class Signature,Start Line,End Line,Fully Qualified Name of Container Class,Class Path\n");

        try (PrintWriter writer = new PrintWriter(csvFilePath)) {
            // First pass: Extract mocked methods from test classes
            Files.walk(Paths.get(projectDir))
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(MockedMethodMocked::extractMockedMethods);

            // Second pass: Search for the container class of mocked methods
            Files.walk(Paths.get(projectDir))
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> searchContainerClass(path, writer));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Print the total number of methods extracted
        System.out.println("Total number of methods extracted: " + methodCount.get());
    }

    // First pass: Extract mocked methods from test files
    private static void extractMockedMethods(Path path) {
        try {
            ParseResult<CompilationUnit> parseResult = new JavaParser().parse(path);
            Optional<CompilationUnit> cu = parseResult.getResult();

            if (cu.isPresent()) {
                CompilationUnit compilationUnit = cu.get();
                compilationUnit.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                    String methodName = methodCallExpr.getNameAsString();
                    if (MOCK_METHODS.containsKey(methodName)) {
                        methodCallExpr.getArguments().forEach(arg -> {
                            if (arg instanceof MethodCallExpr) {
                                MethodCallExpr calledMethod = (MethodCallExpr) arg;
                                // Store mocked method name and class for later searching
                                mockedMethods.put(calledMethod.getNameAsString(), getFullyQualifiedName(path));
                            }
                        });
                    }
                });

                // Also check for MethodReferenceExpr (e.g., FieldConstraintsBuilder::instance)
                compilationUnit.findAll(MethodReferenceExpr.class).forEach(methodRefExpr -> {
                    String methodRefName = methodRefExpr.getIdentifier();
                    String scope = methodRefExpr.getScope().toString();
                    mockedMethods.put(scope + "::" + methodRefName, getFullyQualifiedName(path));
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Second pass: Search for the container class of mocked methods
    private static void searchContainerClass(Path path, PrintWriter writer) {
        try {
            ParseResult<CompilationUnit> parseResult = new JavaParser().parse(path);
            Optional<CompilationUnit> cu = parseResult.getResult();

            if (cu.isPresent()) {
                CompilationUnit compilationUnit = cu.get();
                compilationUnit.findAll(MethodDeclaration.class).forEach(md -> {
                    String methodName = md.getNameAsString();
                    if (mockedMethods.containsKey(methodName)) {
                        // Found the container class for the mocked method
                        String methodSignature = md.getDeclarationAsString();
                        int startLine = md.getBegin().map(pos -> pos.line).orElse(-1);
                        int endLine = md.getEnd().map(pos -> pos.line).orElse(-1);

                        // Adjust the start line if there are annotations
                        if (md.getAnnotations().isNonEmpty()) {
                            startLine += md.getAnnotations().size(); // Adjust start line number
                        }

                        String fullyQualifiedName = getFullyQualifiedName(path);
                        String filePath = path.toString();

                        // Find container class signature
                        String containerClassSignature = getContainerClassSignature(path);

                        // Escape commas by wrapping fields in double quotes
                        String csvEntry = String.format("\"%s\",\"%s\",%d,%d,\"%s\",\"%s\"\n",
                                methodSignature, containerClassSignature, startLine, endLine, fullyQualifiedName, filePath);
                        csvData.append(csvEntry);
                        writer.println(csvEntry);

                        // Increment the method count
                        methodCount.incrementAndGet();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getFullyQualifiedName(Path filePath) {
        // Resolve fully qualified class name from file path
        try {
            CompilationUnit cu = new JavaParser().parse(filePath).getResult().orElse(null);
            if (cu != null) {
                String packageName = cu.getPackageDeclaration()
                        .map(pd -> pd.getNameAsString())
                        .orElse("");
                String className = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                        .map(ClassOrInterfaceDeclaration::getNameAsString)
                        .findFirst()
                        .orElse("");
                return packageName.isEmpty() ? className : packageName + "." + className;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getContainerClassSignature(Path filePath) {
        // Example logic to get the container class signature
        try {
            CompilationUnit cu = new JavaParser().parse(filePath).getResult().orElse(null);
            if (cu != null) {
                String packageName = cu.getPackageDeclaration()
                        .map(pd -> pd.getNameAsString() + ".")
                        .orElse("");
                return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                        .map(classDecl -> packageName + classDecl.getNameAsString())
                        .findFirst()
                        .orElse("");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}