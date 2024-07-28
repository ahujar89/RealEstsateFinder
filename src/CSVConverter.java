import java.io.*;
import java.util.*;

public class CSVConverter {

    private static final Map<String, String> PROVINCE_MAP = createProvinceMap();
    // CSV Data cleaning
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java CSVConverter <input_csv_file> <output_csv_file>");
            return;
        }

        String inputCsvFile = args[0];
        String outputCsvFile = args[1];

        try (BufferedReader br = new BufferedReader(new FileReader(inputCsvFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputCsvFile))) {

            String line;
            // Copy the header line
            if ((line = br.readLine()) != null) {
                bw.write(line);
                bw.newLine();
            }

            // Convert the provinces and copy the rest of the lines
            while ((line = br.readLine()) != null) {
                String[] columns = parseCSVLine(line);
                if (columns.length > 3) { // Ensure there are enough columns
                    columns[3] = getFullProvinceName(columns[3]);
                    bw.write(String.join(",", columns));
                    bw.newLine();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("CSV conversion completed successfully.");
    }

    private static Map<String, String> createProvinceMap() {
        Map<String, String> map = new HashMap<>();
        map.put("ON", "Ontario");
        map.put("AB", "Alberta");
        map.put("BC", "British Columbia");
        map.put("MB", "Manitoba");
        map.put("NB", "New Brunswick");
        map.put("NL", "Newfoundland and Labrador");
        map.put("NS", "Nova Scotia");
        map.put("NT", "Northwest Territories");
        map.put("NU", "Nunavut");
        map.put("PE", "Prince Edward Island");
        map.put("QC", "Quebec");
        map.put("SK", "Saskatchewan");
        map.put("YT", "Yukon");
        return map;
    }

    private static String getFullProvinceName(String abbreviation) {
        return PROVINCE_MAP.getOrDefault(abbreviation, abbreviation);
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
}
