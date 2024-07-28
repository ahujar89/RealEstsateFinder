import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Bed and Bath listing filtering
public class FilterByBedBath {
    // Updated pattern to handle both "n bed" and "n+1 bed" formats
    private static final Pattern BED_BATH_PATTERN = Pattern.compile("(\\d+\\+?\\d*) bed (\\d+) bath");

    public static void filter(Scanner scanner, String csvFilePath) {
        int beds = -1, baths = -1;

        while (beds < 0) {
            System.out.print("\033[1;36mEnter number of bedrooms:\033[0m ");
            String bedsStr = scanner.nextLine().trim();
            try {
                beds = Integer.parseInt(bedsStr);
                if (beds < 0) {
                    System.out.println("\033[1;31mInvalid input. Please enter a positive number.\033[0m");
                    beds = -1;
                }
            } catch (NumberFormatException e) {
                System.out.println("\033[1;31mInvalid input. Please enter a valid number.\033[0m");
            }
        }

        while (baths < 0) {
            System.out.print("\033[1;36mEnter number of bathrooms:\033[0m ");
            String bathsStr = scanner.nextLine().trim();
            try {
                baths = Integer.parseInt(bathsStr);
                if (baths < 0) {
                    System.out.println("\033[1;31mInvalid input. Please enter a positive number.\033[0m");
                    baths = -1;
                }
            } catch (NumberFormatException e) {
                System.out.println("\033[1;31mInvalid input. Please enter a valid number.\033[0m");
            }
        }

        List<String[]> filteredListings = filterByBedBath(csvFilePath, beds, baths);

        System.out.println("Total listings: " + filteredListings.size());
        System.out.println("\033[1;32mFiltered listings:\033[0m");
        for (String[] listing : filteredListings) {
            System.out.println("Price: " + listing[0]);
            System.out.println("Address: " + listing[1]);
            System.out.println("City: " + listing[2]);
            System.out.println("Province: " + listing[3]);
            System.out.println("Details: " + listing[4]);
            System.out.println("URL: " + listing[5]);
            if (listing.length > 6) {
                System.out.println("Image File: " + listing[6]);
            }
            System.out.println();
        }
    }

    private static List<String[]> filterByBedBath(String csvFilePath, int beds, int baths) {
        List<String[]> filteredListings = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            br.readLine(); // Skip the header line
            while ((line = br.readLine()) != null) {
                String[] columns = parseCSVLine(line);
                if (columns.length >= 6) {
                    Matcher matcher = BED_BATH_PATTERN.matcher(columns[4]);
                    if (matcher.find()) {
                        String bedGroup = matcher.group(1);
                        int bedCount = parseBedCount(bedGroup);
                        int bathCount = Integer.parseInt(matcher.group(2));
                        if ((bedCount == beds) || (beds + 1 == bedCount && bedGroup.contains("+"))) {
                            if (bathCount == baths) {
                                filteredListings.add(columns);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filteredListings;
    }

    private static int parseBedCount(String bedGroup) {
        if (bedGroup.contains("+")) {
            String[] parts = bedGroup.split("\\+");
            return Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]);
        } else {
            return Integer.parseInt(bedGroup);
        }
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
