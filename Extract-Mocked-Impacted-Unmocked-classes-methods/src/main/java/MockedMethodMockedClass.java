import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MockedMethodMockedClass {

    public static void main(String[] args) {
        String allMethodsCsv = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/MockedClassAllMethods.csv";
        String mockedMethodsCsv = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Mocked_Method_Mocked.csv";
        String outputCsv = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Final/Mocked_Method_Mocked.csv";

        try {
            Map<String, String> allMethodsMap = loadCSVToMap(allMethodsCsv);
            matchAndWriteCSV(mockedMethodsCsv, allMethodsMap, outputCsv);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> loadCSVToMap(String csvFile) throws IOException {
        Map<String, String> methodsMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            // Read header
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(",");
                if (columns.length > 4) { // Ensure we have at least 5 columns
                    String methodSignature = columns[0];
                    String fullyQualifiedClassName = columns[4];
                    String key = methodSignature + "###" + fullyQualifiedClassName;
                    methodsMap.put(key, line);
                }
            }
        }
        return methodsMap;
    }

    private static void matchAndWriteCSV(String mockedCsv, Map<String, String> allMethodsMap, String outputCsv) throws IOException {
        Set<String> uniqueRows = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(mockedCsv));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputCsv))) {
            String line;
            // Write header to output CSV
            bw.write("Method Signature,Container Class Signature,Start Line,End Line,Fully Qualified Name of Container Class,Class Path,Method Label");
            bw.newLine();

            // Read mocked methods CSV
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] columns = line.split(",");
                if (columns.length > 4) { // Ensure we have at least 5 columns
                    String methodSignature = columns[0];
                    String fullyQualifiedClassName = columns[4];
                    String key = methodSignature + "###" + fullyQualifiedClassName;

                    if (allMethodsMap.containsKey(key)) {
                        String rowToWrite = allMethodsMap.get(key) + ",Mocked";
                        if (uniqueRows.add(rowToWrite)) {
                            bw.write(rowToWrite);
                            bw.newLine();
                        }
                    }
                }
            }
        }
    }
}
