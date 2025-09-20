import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MockedMethodMockedClassChecker {

    private static final AtomicInteger methodCount = new AtomicInteger(0);
    private static final Map<String, String> MOCK_METHODS = new HashMap<>();
    private static final StringBuilder csvData = new StringBuilder();

    // Stores mocked methods and their fully qualified class
    private static final Map<String, String> mockedMethods = new HashMap<>();

    // Stores whether a mocked method is tested somewhere (true = tested, false = only mocked)
    private static final Map<String, Boolean> mockedMethodTestedMap = new HashMap<>();

    // New: Stores the file path where each mocked method was found
    private static final Map<String, String> mockedMethodFilePathMap = new HashMap<>();

    static {
        MOCK_METHODS.put("when", "org.mockito.Mockito.when");
        MOCK_METHODS.put("given", "org.mockito.BDDMockito.given");
        MOCK_METHODS.put("defaultAnswer", "org.mockito.Mockito.defaultAnswer");
        MOCK_METHODS.put("lenient", "org.mockito.Mockito.lenient");
        MOCK_METHODS.put("mockStatic", "org.mockito.Mockito.mockStatic");
    }

    public static void main(String[] args) {
        String projectDir = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Projects/cucumber-jvm"; // Update path
        String csvFilePath = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Mocked_Method_Mocked_Checker.csv";

        // Add CSV header with new "Mocked In File Path" column
        csvData.append("Method Signature,Container Class Signature,Start Line,End Line,Fully Qualified Name of Container Class,Class Path,Mocked In File Path,Method Label\n");

        try (PrintWriter writer = new PrintWriter(csvFilePath)) {
            writer.println("Method Signature,Container Class Signature,Start Line,End Line,Fully Qualified Name of Container Class,Class Path,Mocked In File Path,Method Label");

            Files.walk(Paths.get(projectDir))
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(MockedMethodMockedClassChecker::extractMockedMethods);

            mockedMethods.keySet().forEach(m -> mockedMethodTestedMap.put(m, false));

            Files.walk(Paths.get(projectDir))
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> searchContainerClass(path, writer));

            Files.walk(Paths.get(projectDir))
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(MockedMethodMockedClassChecker::markTestedMockedMethods);

            rewriteCsvWithTestStatus(csvFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Total number of methods extracted: " + methodCount.get());
    }

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
                                String mockedMethod = calledMethod.getNameAsString();
                                mockedMethods.put(mockedMethod, getFullyQualifiedName(path));
                                mockedMethodFilePathMap.put(mockedMethod, path.toString());
                            }
                        });
                    }
                });

                compilationUnit.findAll(MethodReferenceExpr.class).forEach(methodRefExpr -> {
                    String methodRefName = methodRefExpr.getIdentifier();
                    String scope = methodRefExpr.getScope().toString();
                    String fullRef = scope + "::" + methodRefName;
                    mockedMethods.put(fullRef, getFullyQualifiedName(path));
                    mockedMethodFilePathMap.put(fullRef, path.toString());
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void searchContainerClass(Path path, PrintWriter writer) {
        try {
            ParseResult<CompilationUnit> parseResult = new JavaParser().parse(path);
            Optional<CompilationUnit> cu = parseResult.getResult();

            if (cu.isPresent()) {
                CompilationUnit compilationUnit = cu.get();
                compilationUnit.findAll(MethodDeclaration.class).forEach(md -> {
                    String methodName = md.getNameAsString();
                    if (mockedMethods.containsKey(methodName)) {
                        String methodSignature = md.getDeclarationAsString();
                        int startLine = md.getBegin().map(pos -> pos.line).orElse(-1);
                        int endLine = md.getEnd().map(pos -> pos.line).orElse(-1);

                        if (md.getAnnotations().isNonEmpty()) {
                            startLine += md.getAnnotations().size();
                        }

                        String fullyQualifiedName = getFullyQualifiedName(path);
                        String filePath = path.toString();
                        String containerClassSignature = getContainerClassSignature(path);

                        String mockedInFilePath = mockedMethodFilePathMap.getOrDefault(methodName, "Unknown");

                        String csvEntry = String.format("\"%s\",\"%s\",%d,%d,\"%s\",\"%s\",\"%s\",\"NotTested\"\n",
                                methodSignature, containerClassSignature, startLine, endLine, fullyQualifiedName, filePath, mockedInFilePath);
                        csvData.append(csvEntry);
                        writer.println(csvEntry);

                        methodCount.incrementAndGet();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void markTestedMockedMethods(Path path) {
        try {
            ParseResult<CompilationUnit> parseResult = new JavaParser().parse(path);
            Optional<CompilationUnit> cu = parseResult.getResult();

            if (cu.isPresent()) {
                CompilationUnit compilationUnit = cu.get();
                compilationUnit.findAll(MethodDeclaration.class).forEach(md -> {
                    boolean isTestMethod = md.getAnnotations().stream()
                            .anyMatch(a -> a.getNameAsString().equals("Test"));
                    if (isTestMethod) {
                        md.findAll(MethodCallExpr.class).forEach(call -> {
                            String calledMethodName = call.getNameAsString();
                            if (mockedMethodTestedMap.containsKey(calledMethodName)) {
                                mockedMethodTestedMap.put(calledMethodName, true);
                            }
                        });
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void rewriteCsvWithTestStatus(String csvFilePath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(csvFilePath));
            List<String> updatedLines = new ArrayList<>();

            if (!lines.isEmpty()) {
                updatedLines.add(lines.get(0));
            }

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                int firstQuote = line.indexOf('"');
                int secondQuote = line.indexOf('"', firstQuote + 1);
                if (firstQuote >= 0 && secondQuote > firstQuote) {
                    String methodSignature = line.substring(firstQuote + 1, secondQuote);
                    String methodName = extractMethodNameFromSignature(methodSignature);
                    Boolean tested = mockedMethodTestedMap.get(methodName);
                    String status = (tested != null && tested) ? "Tested" : "NotTested";
                    String updatedLine = line.replaceAll("\"NotTested\"|\"Tested\"", "\"" + status + "\"");
                    updatedLines.add(updatedLine);
                } else {
                    updatedLines.add(line);
                }
            }

            Files.write(Paths.get(csvFilePath), updatedLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String extractMethodNameFromSignature(String methodSignature) {
        methodSignature = methodSignature.trim();
        int parenIndex = methodSignature.indexOf('(');
        if (parenIndex > 0) {
            String beforeParen = methodSignature.substring(0, parenIndex).trim();
            int lastSpace = beforeParen.lastIndexOf(' ');
            if (lastSpace >= 0 && lastSpace < beforeParen.length() - 1) {
                return beforeParen.substring(lastSpace + 1);
            } else {
                return beforeParen;
            }
        }
        return methodSignature;
    }

    private static String getFullyQualifiedName(Path filePath) {
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
