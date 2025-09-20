import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class FullyQualifiedClassExtractor {
    private static AtomicInteger classCount = new AtomicInteger(0);
    private static StringBuilder csvData = new StringBuilder();
    private static Path projectDir;

    public static void main(String[] args) {
        projectDir = Paths.get("//Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Projects/cucumber-jvm");

        try {
            Files.walk(projectDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(FullyQualifiedClassExtractor::parse);

            System.out.println("Total number of classes: " + classCount.get());

            try (FileWriter writer = new FileWriter("/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Fully_Qualified_Classes.csv")) {
                writer.write(csvData.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parse(Path path) {
        try {
            ParseResult<CompilationUnit> parseResult = new JavaParser().parse(path);
            Optional<CompilationUnit> cu = parseResult.getResult();

            if (cu.isPresent()) {
                CompilationUnit compilationUnit = cu.get();
                String packageName = compilationUnit.getPackageDeclaration()
                        .map(pd -> pd.getName().toString())
                        .orElse("");

                VoidVisitor<Path> classVisitor = new ClassSignaturePrinter(packageName, path);
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
        private final Path classPath;

        public ClassSignaturePrinter(String packageName, Path classPath) {
            this.packageName = packageName;
            this.classPath = classPath;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration cid, Path path) {
            super.visit(cid, path);
            String className = cid.getName().toString();
            String fullyQualifiedName = getFullyQualifiedName(classPath.toString(), className);
            String csvEntry = fullyQualifiedName + ".class," + path.toString() + "\n";
            csvData.append(csvEntry);

            System.out.println(fullyQualifiedName + ".class" + "    " + path.toString());

            classCount.incrementAndGet();
        }

        private String getFullyQualifiedName(String classPath, String className) {
            if (classPath == null || className == null) {
                return null;
            }

            Path path = Paths.get(classPath);
            Path parent = path.getParent();
            if (parent == null) {
                return null;
            }

            String relativePath = projectDir.relativize(parent).toString();
            String packageName = relativePath.replace('/', '.').replace('\\', '.');
            return packageName.isEmpty() ? className : packageName + "." + className;
        }
    }
}