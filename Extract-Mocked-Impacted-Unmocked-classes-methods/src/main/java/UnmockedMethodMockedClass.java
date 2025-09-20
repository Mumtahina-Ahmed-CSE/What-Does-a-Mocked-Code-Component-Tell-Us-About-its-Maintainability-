import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class UnmockedMethodMockedClass {

    public static void main(String[] args) {
        String allMethodsCsv = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/MockedClassAllMethods.csv";
        String mockedMethodsCsv = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Final/Mocked_Method_Mocked.csv";
        String unmatchedCsv = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Final/UnmockedMethodMocked.csv";

        try {
            Set<String> mockedMethodsSet = loadCSVRowsToSet(mockedMethodsCsv);
            int totalUnmockedMethods = findAndWriteUnmatchedRows(allMethodsCsv, mockedMethodsSet, unmatchedCsv);
            System.out.println("Total number of unmocked methods: " + totalUnmockedMethods);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Set<String> loadCSVRowsToSet(String csvFile) throws IOException {
        Set<String> rowsSet = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            br.readLine(); // Skip the header
            while ((line = br.readLine()) != null) {
                rowsSet.add(line.trim());
            }
        }
        return rowsSet;
    }

    private static int findAndWriteUnmatchedRows(String allMethodsCsv, Set<String> mockedMethodsSet, String unmatchedCsv) throws IOException {
        int unmockedCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(allMethodsCsv));
             BufferedWriter bw = new BufferedWriter(new FileWriter(unmatchedCsv))) {

            String line;
            bw.write("Method Signature,Container Class Signature,Start Line,End Line,Fully Qualified Name of Container Class,Class Path, Method Label");
            bw.newLine();

            br.readLine(); // Skip the header
            while ((line = br.readLine()) != null) {
                if (!mockedMethodsSet.contains(line.trim())) {
                    bw.write(line + ",Unmocked");
                    bw.newLine();
                    unmockedCount++;
                }
            }
        }
        return unmockedCount;
    }
}