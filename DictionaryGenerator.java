import java.io.*;
import java.nio.file.*;
import java.util.*;

public class DictionaryGenerator {

    // Generate Dictionary keywords
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java DictionaryGenerator <input_csv_file> <output_dictionary_file>");
            return;
        }

        String csvFile = args[0];
        String dictionaryFile = args[1];

        // Ensure the resources directory exists
        File resourcesDir = new File(dictionaryFile).getParentFile();
        if (resourcesDir != null && !resourcesDir.exists()) {
            resourcesDir.mkdirs();
        }

        Set<String> dictionary = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            br.readLine(); // Skip the header line
            while ((line = br.readLine()) != null) {
                String[] columns = parseCSVLine(line);
                if (columns.length > 3) { // Ensure there are enough columns
                    addEntryToDictionary(columns[2], dictionary); // City name
                    addEntryToDictionary(columns[3], dictionary); // Province name
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dictionaryFile))) {
            for (String word : dictionary) {
                bw.write(word);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Dictionary generated successfully.");
    }

    private static String[] parseCSVLine(String line) {
        List<String> columns = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char ch : line.toCharArray()) {
            switch (ch) {
                case '"':
                    inQuotes = !inQuotes;
                    break;
                case ',':
                    if (inQuotes) {
                        sb.append(ch);
                    } else {
                        columns.add(sb.toString());
                        sb.setLength(0);
                    }
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }
        columns.add(sb.toString());
        return columns.toArray(new String[0]);
    }

    private static void addEntryToDictionary(String text, Set<String> dictionary) {
        if (!text.isEmpty()) {
            dictionary.add(text.toLowerCase());
        }
    }
}
