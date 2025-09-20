import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MethodExtractor {

    public static void main(String[] args) {
        String projectDir = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Projects/cucumber-jvm"; // Update this path

        try {
            Files.walk(Paths.get(projectDir))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> {
                        List<MethodInfo> methods = parse(path);
                        methods.forEach(System.out::println); // Just to show the methods
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static List<MethodInfo> parse(Path path) {
        List<MethodInfo> methodInfos = new ArrayList<>();

        try {
            // Parse the Java file
            ParseResult<CompilationUnit> parseResult = new JavaParser().parse(path);

            // Check if the result contains a CompilationUnit
            Optional<CompilationUnit> cu = parseResult.getResult();

            // If present, visit the CompilationUnit
            if (cu.isPresent()) {
                MethodNameCollector methodNameCollector = new MethodNameCollector(path);
                methodNameCollector.visit(cu.get(), methodInfos);
            } else {
                System.err.println("Failed to parse file: " + path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return methodInfos;
    }

    private static class MethodNameCollector extends VoidVisitorAdapter<List<MethodInfo>> {
        private final Path filePath;

        public MethodNameCollector(Path filePath) {
            this.filePath = filePath;
        }

        @Override
        public void visit(MethodDeclaration md, List<MethodInfo> methodInfos) {
            super.visit(md, methodInfos);

            // Collect method information
            String methodName = md.getNameAsString();
            String returnType = md.getTypeAsString();

            // Format the parameter list
            StringBuilder parameters = new StringBuilder();
            md.getParameters().forEach(param -> {
                if (parameters.length() > 0) {
                    parameters.append(", ");
                }
                parameters.append(param.getType().asString()).append(" ").append(param.getNameAsString());
            });

            // Format the method signature (without modifiers)
            String methodSignature = String.format("%s %s(%s)", returnType, methodName, parameters.toString());

            // Escape any commas in the method signature
            methodSignature = escapeCsvValue(methodSignature);

            // Get the start and end line numbers, adjusting for annotations
            Optional<Position> begin = md.getBegin();
            Optional<Position> end = md.getEnd();
            int startLine = begin.map(position -> position.line).orElse(-1);
            int endLine = end.map(position -> position.line).orElse(-1);

            // If there are annotations, adjust the start line to be after the annotations
            if (md.getAnnotations().isNonEmpty()) {
                startLine += md.getAnnotations().size();
            }

            // Find the containing class and get its signature
            Optional<ClassOrInterfaceDeclaration> containerClass = md.findAncestor(ClassOrInterfaceDeclaration.class);
            if (containerClass.isPresent()) {
                String classSignature = getClassSignature(containerClass.get());
                String fullyQualifiedName = getFullyQualifiedName(containerClass.get());

                // Add method info to the list
                methodInfos.add(new MethodInfo(methodSignature, classSignature, startLine, endLine, fullyQualifiedName, filePath.toString()));
            }
        }

        private String getClassSignature(ClassOrInterfaceDeclaration cd) {
            // Extract class modifiers
            String modifiers = cd.getModifiers().toString().replaceAll("[\\[\\]]", "");

            // Extract class name
            String className = cd.getNameAsString();

            // Extract class type parameters (generic types)
            StringBuilder typeParameters = new StringBuilder();
            if (!cd.getTypeParameters().isEmpty()) {
                typeParameters.append("<");
                for (int i = 0; i < cd.getTypeParameters().size(); i++) {
                    if (i > 0) {
                        typeParameters.append(", ");
                    }
                    typeParameters.append(cd.getTypeParameters().get(i).toString());
                }
                typeParameters.append(">");
            }

            // Format the full class signature
            return String.format("%s class %s%s", modifiers, className, typeParameters.toString());
        }

        private String getFullyQualifiedName(ClassOrInterfaceDeclaration classDeclaration) {
            Optional<CompilationUnit> cu = classDeclaration.findCompilationUnit();
            String packageName = cu.flatMap(CompilationUnit::getPackageDeclaration)
                    .map(pkg -> pkg.getName().toString())
                    .orElse("");
            return packageName.isEmpty() ? classDeclaration.getNameAsString() : packageName + "." + classDeclaration.getNameAsString();
        }

        // Escape CSV values to prevent issues with commas
        private String escapeCsvValue(String value) {
            if (value.contains(",")) {
                return "\"" + value.replace("\"", "\"\"") + "\"";
            }
            return value;
        }
    }

    // MethodInfo class to hold method details
    static class MethodInfo {
        String methodSignature;
        String classSignature;
        int startLine;
        int endLine;
        String fullyQualifiedName;
        String filePath;

        public MethodInfo(String methodSignature, String classSignature, int startLine, int endLine, String fullyQualifiedName, String filePath) {
            this.methodSignature = methodSignature;
            this.classSignature = classSignature;
            this.startLine = startLine;
            this.endLine = endLine;
            this.fullyQualifiedName = fullyQualifiedName;
            // this.filePath = filePath;
        }

        @Override
        public String toString() {
            return  "methodSignature='" + methodSignature + '\'' +
                    ", classSignature='" + classSignature + '\'' +
                    ", startLine=" + startLine +
                    ", endLine=" + endLine +
                    ", fullyQualifiedName='" + fullyQualifiedName + '\'' +
                    // ", filePath='" + filePath + '\'' +
                    '}';
        }
    }
}