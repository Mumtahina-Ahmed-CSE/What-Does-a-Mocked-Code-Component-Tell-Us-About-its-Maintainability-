import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CsvModifier {

    public static void main(String[] args) {
        String mockedMethodsCsv = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cassandra-Output/Final/CodeShovel/Input/4.csv";
        String outputCsv = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cassandra-Output/Final/CodeShovel/Input/UnmockedMethod4.csv";
        // String outputCsv = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/Metanome-Output/CodeShovel/Input/MockedMethodMocked.csv";
        // String outputCsv = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/Metanome-Output/Final/CodeShovel/Input/ImpactedByMocking.csv";
        // String outputCsv = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/shardingsphere-elasticjob-Output/CodeShovel/Input/UnmockedMethod.csv";
        
        // Define the part of the path to remove
        String pathToRemove = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Projects/cassandra/";
        
        modifyCsv(mockedMethodsCsv, outputCsv, pathToRemove);
    }

    public static void modifyCsv(String inputCsv, String outputCsv, String pathToRemove) {
        try (
                CSVReader reader = new CSVReader(new FileReader(inputCsv));
                CSVWriter writer = new CSVWriter(new FileWriter(outputCsv))
        ) {
            String[] nextLine;

            while ((nextLine = reader.readNext()) != null) {
                // Skip empty lines or lines with insufficient columns
                if (nextLine.length == 0) {
                    continue; // Skip empty lines
                }

                if (nextLine.length < 6) {
                    System.err.println("Warning: Line does not have enough columns: " + String.join(",", nextLine));
                    continue; // Skip lines that do not have at least 6 columns
                }

                // Modify the Class Path column (assuming it's the 6th column, index 5)
                String classPath = nextLine[5];
                if (classPath.startsWith(pathToRemove)) {
                    // Remove the specific portion of the path
                    nextLine[5] = classPath.replaceFirst(pathToRemove, "");
                }

                // Write the modified or unmodified row to the output CSV
                writer.writeNext(nextLine);
            }

            System.out.println("CSV modification complete. Output saved to: " + outputCsv);

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }
}