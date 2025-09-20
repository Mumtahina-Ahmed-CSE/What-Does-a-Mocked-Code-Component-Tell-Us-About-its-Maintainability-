import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class UnmockedClassesParser {

    public static void main(String[] args) {
        String mockedClassesFile = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Final/Mocked_classes.csv";
        String allClassesFile = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Fully_Qualified_Classes.csv";
        String unmockedClassesOutputFile = "/Users/mumtahinaahmed/Desktop/Mockito/Mockito-Project/Mocked-notMocked/src/Projects-Output/cucumber-jvm-Output/Final/Unmocked_Classes.csv";

        Set<String> mockedClassNames = new HashSet<>();
        Set<String> unmockedClasses = new HashSet<>();
        int unmockedClassCount = 0;

        // Read the mocked classes and store their names in a set
        try (BufferedReader br = new BufferedReader(new FileReader(mockedClassesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String className = values[0].trim();
                mockedClassNames.add(className);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Read all classes and add unmatched classes to the unmockedClasses set
        try (BufferedReader br = new BufferedReader(new FileReader(allClassesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 2) {
                    // Optionally print a warning or skip silently
                    System.out.println("Skipping malformed line: " + line);
                    continue;
                }
                String className = values[0].trim();
                String classPath = values[1].trim();
                if (!mockedClassNames.contains(className)) {
                    unmockedClasses.add(className + ", " + classPath + ", unmocked");
                    unmockedClassCount++; // Increment the counter
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Save unmocked classes to a CSV file
        try (FileWriter writer = new FileWriter(unmockedClassesOutputFile)) {
            writer.append("Fully Qualified Unmocked Class Name, Unmocked Class Path, Class Label\n");
            for (String unmockedClass : unmockedClasses) {
                writer.append(unmockedClass).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Print out the unmocked classes and the count
        System.out.println("Unmocked Classes:");
        for (String unmockedClass : unmockedClasses) {
            System.out.println(unmockedClass);
        }
        System.out.println("Total Unmocked Classes: " + unmockedClassCount);
    }
}