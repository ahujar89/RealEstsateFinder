import java.util.*;
import java.io.*;

// Filter listings by price
public class FilterByPrice {
    public static void filter(Scanner scanner, String csvFilePath, String selectedCityOrProvince) {
        double minPrice = 0;
        double maxPrice = 0;

        while (true) {
            // Input validation for minimum price
            while (true) {
                System.out.print("\033[1;36mEnter minimum price :\033[0m ");
                if (scanner.hasNextDouble()) {
                    minPrice = scanner.nextDouble();
                    if (minPrice >= 0) {
                        break;
                    } else {
                        System.out.println("\033[1;31mInvalid input. Price cannot be negative.\033[0m");
                    }
                } else {
                    System.out.println("\033[1;31mInvalid input. Please enter a valid number.\033[0m");
                    scanner.next(); // Consume invalid input
                }
            }

            // Input validation for maximum price
            while (true) {
                System.out.print("\033[1;36mEnter maximum price :\033[0m ");
                if (scanner.hasNextDouble()) {
                    maxPrice = scanner.nextDouble();
                    if (maxPrice >= 0) {
                        break;
                    } else {
                        System.out.println("\033[1;31mInvalid input. Price cannot be negative.\033[0m");
                    }
                } else {
                    System.out.println("\033[1;31mInvalid input. Please enter a valid number.\033[0m");
                    scanner.next(); // Consume invalid input
                }
            }
            scanner.nextLine(); // Consume newline

            if (minPrice > maxPrice) {
                System.out.println("\033[1;31mMinimum price cannot be greater than maximum price. Please try again.\033[0m");
            } else {
                break;
            }
        }

        List<String[]> filteredListings = filterByPrice(csvFilePath, minPrice, maxPrice, selectedCityOrProvince);

        System.out.println("Total listings in your budget: " + filteredListings.size());
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

    // Method to filter listings by price range and city or province
    private static List<String[]> filterByPrice(String csvFilePath, double minPrice, double maxPrice, String selectedCityOrProvince) {
        List<String[]> filteredListings = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            br.readLine(); // Skip the header line
            while ((line = br.readLine()) != null) {
                String[] columns = parseCSVLine(line);
                if (columns.length >= 6) {
                    try {
                        // Clean and parse the price
                        String cleanedPrice = columns[0].replaceAll("[^\\d.]", "");
                        double price = Double.parseDouble(cleanedPrice);
                        if (price >= minPrice && price <= maxPrice) {
                            // Check if the listing matches the selected city or province
                            if (selectedCityOrProvince == null || selectedCityOrProvince.isEmpty() ||
                                    selectedCityOrProvince.equalsIgnoreCase(columns[2].trim()) ||
                                    selectedCityOrProvince.equalsIgnoreCase(columns[3].trim())) {
                                filteredListings.add(columns);
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("\033[1;31mSkipping invalid price format: " + columns[0] + "\033[0m");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filteredListings;
    }

    // Method to parse a CSV line considering quoted fields with commas
    private static String[] parseCSVLine(String line) {
        List<String> columns = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            switch (c) {
                case '"':
                    inQuotes = !inQuotes;
                    break;
                case ',':
                    if (inQuotes) {
                        sb.append(c);
                    } else {
                        columns.add(sb.toString());
                        sb.setLength(0);
                    }
                    break;
                default:
                    sb.append(c);
            }
        }
        columns.add(sb.toString()); // Add the last column
        return columns.toArray(new String[0]);
    }
}
