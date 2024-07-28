import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class TorontoDataCleaner {
    // Clean data from Toronto listings - > remove municipality/noeighbourhood code
    public static void torontoDataCleaner(String inputCsvFilePath) {
        List<String[]> allLines;
        try (CSVReader reader = new CSVReader(new FileReader(inputCsvFilePath))) {
            allLines = reader.readAll();
        } catch (IOException | CsvException e) {
            System.out.println("Error reading CSV file: " + e.getMessage());
            return;
        }

        // Assuming the first row is the header
        if (allLines.isEmpty()) {
            return;
        }

        // Regex pattern to match "Toronto" followed by a letter and a digit
        Pattern torontoPattern = Pattern.compile("Toronto [A-Z]\\d+");

        // Process each listing
        for (int i = 1; i < allLines.size(); i++) {
            String[] line = allLines.get(i);
            String city = line[2]; // Assuming the city is in the third column

            if (torontoPattern.matcher(city).matches()) {
                line[2] = "Toronto";
            }
        }

        // Overwrite the original CSV file with the updated data
        try (CSVWriter writer = new CSVWriter(new FileWriter(inputCsvFilePath))) {
            writer.writeAll(allLines);
        } catch (IOException e) {
            System.out.println("Error writing CSV file: " + e.getMessage());
        }
    }
}
