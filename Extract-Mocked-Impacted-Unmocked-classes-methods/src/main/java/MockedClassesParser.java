import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MockedClassesParser {

    private static AtomicInteger mockedClassCount = new AtomicInteger(0); // Thread-safe counter
    private static final Map<String, String> MOCK_METHODS = new HashMap<>();
    private static StringBuilder csvData = new StringBuilder(); // To collect CSV data
    private static final Set<String> uniqueMockedEntries = new HashSet<>(); // For ensuring uniqueness

    static {
        MOCK_METHODS.put("mock", "org.mockito.Mockito.mock");
        MOCK_METHODS.put("spy", "org.mockito.Mockito.spy");
        MOCK_METHODS.put("@Mock", "org.mockito.Mock");
        MOCK_METHODS.put("mockStatic", "org.mockito.Mockito.mockStatic");
        MOCK_METHODS.put("mockConstruction", "org.mockito.Mockito.mockConstruction");
        MOCK_METHODS.put("MockBean", "org.springframework.context.annotation.Bean");
        MOCK_METHODS.put("@InjectMocks", "org.mockito.InjectMocks");
        MOCK_METHODS.put("Mock.Strictness", "org.mockito.Mock.Strictness");
        MOCK_METHODS.put("initMocks", "org.mockito.MockitoAnnotations.initMocks");
    }

    public static void main(String[] args) {
        String projectDir = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Projects/cucumber-jvm"; // Update this path
        String csvFilePath = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Final/Mocked_classes.csv"; // Output CSV

        // Add CSV header
        csvData.append("Fully Qualified Mocked Class Name,Mocked Class Path,Label\n");

        try (PrintWriter writer = new PrintWriter(csvFilePath)) {
        	writer.println("Fully Qualified Mocked Class Name,Mocked Class Path,Label"); 
            Files.walk(Paths.get(projectDir))
                    .filter(p -> p.toString().contains("/test/") && p.toString().endsWith(".java")) // Test files only
                    .forEach(path -> parse(path, writer, projectDir));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Print total unique mocked class count
        System.out.println("Total number of unique mocked classes: " + mockedClassCount.get());
    }

    private static void parse(Path path, PrintWriter writer, String projectDir) {
        try {
            ParseResult<CompilationUnit> parseResult = new JavaParser().parse(path);
            Optional<CompilationUnit> cu = parseResult.getResult();

            if (cu.isPresent()) {
                CompilationUnit compilationUnit = cu.get();
                String packageName = compilationUnit.getPackageDeclaration()
                        .map(pd -> pd.getName().toString())
                        .orElse("");

                VoidVisitor<Path> classVisitor = new ClassSignaturePrinter(packageName, writer, Paths.get(projectDir));
                classVisitor.visit(compilationUnit, path);
            } else {
                System.err.println("Failed to parse file: " + path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClassSignaturePrinter extends VoidVisitorAdapter<Path> {
        private final String packageName;
        private final PrintWriter writer;
        private final Path projectDir;

        public ClassSignaturePrinter(String packageName, PrintWriter writer, Path projectDir) {
            this.packageName = packageName;
            this.writer = writer;
            this.projectDir = projectDir;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration cid, Path path) {
            super.visit(cid, path);

            // Check field declarations for @Mock
            cid.findAll(FieldDeclaration.class).forEach(fieldDeclaration -> {
                if (fieldDeclaration.isAnnotationPresent("Mock")) {
                    String mockedClassName = fieldDeclaration.getElementType().asString();
                    String mockedClassPath = findMockedClassPath(mockedClassName);
                    String fullyQualifiedName = getFullyQualifiedName(mockedClassPath, mockedClassName);

                    if (mockedClassPath != null && fullyQualifiedName != null) {
                        String csvEntry = fullyQualifiedName + ".class," + mockedClassPath + ",mocked";

                        if (uniqueMockedEntries.add(csvEntry)) {
                            csvData.append(csvEntry);
                            writer.println(csvEntry);
                            System.out.println(fullyQualifiedName + ".class" + "    " + mockedClassPath);
                            mockedClassCount.incrementAndGet();
                        }
                    }
                }
            });

            // Check for mocked method calls like mock(), spy(), etc.
            cid.findAll(MethodCallExpr.class).forEach(methodCallExpr -> {
                String methodName = methodCallExpr.getNameAsString();

                if (MOCK_METHODS.containsKey(methodName)) {
                    Optional<ClassExpr> classExprOpt = methodCallExpr.getArguments().stream()
                            .filter(arg -> arg instanceof ClassExpr)
                            .map(arg -> (ClassExpr) arg)
                            .findFirst();

                    if (classExprOpt.isPresent()) {
                        String mockedClassName = classExprOpt.get().getType().asString();
                        String mockedClassPath = findMockedClassPath(mockedClassName);
                        String fullyQualifiedName = getFullyQualifiedName(mockedClassPath, mockedClassName);

                        if (mockedClassPath != null && fullyQualifiedName != null) {
                            String csvEntry = fullyQualifiedName + ".class," + mockedClassPath + ",mocked";

                            if (uniqueMockedEntries.add(csvEntry)) {
                                csvData.append(csvEntry);
                                writer.println(csvEntry);
                                System.out.println(fullyQualifiedName + ".class" + "    " + mockedClassPath);
                                mockedClassCount.incrementAndGet();
                            }
                        }
                    }
                }
            });
        }

        private String findMockedClassPath(String mockedClassName) {
            try {
                return Files.walk(projectDir)
                        .filter(p -> p.toString().endsWith(mockedClassName.replace('.', '/') + ".java"))
                        .map(Path::toString)
                        .findFirst()
                        .orElse(null);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        private String getFullyQualifiedName(String mockedClassPath, String mockedClassName) {
            if (mockedClassPath == null || mockedClassName == null) return null;

            Path path = Paths.get(mockedClassPath);
            Path parent = path.getParent();
            if (parent == null) return null;

            String relativePath = projectDir.relativize(parent).toString();
            String packageName = relativePath.replace('/', '.').replace('\\', '.');
            return packageName.isEmpty() ? mockedClassName : packageName + "." + mockedClassName;
        }
    }
}