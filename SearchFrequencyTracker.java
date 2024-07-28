import java.io.*;
import java.util.*;

public class SearchFrequencyTracker {
    // File name where search frequencies are stored
    public static final String FILE_NAME = "search_frequencies.txt";

    // HashMap to store search queries and their frequencies
    private Map<String, Integer> searchFrequencies;

    // Constructor to initialize the HashMap and load data from the file
    public SearchFrequencyTracker() {
        searchFrequencies = new HashMap<>();
        loadFrequenciesFromFile();
    }

    // Method to handle search and update the frequency of the search query
    public int search(String ptrn) {
        ptrn = ptrn.toLowerCase(); // Convert the search pattern to lowercase
        // Use KMP to check if the ptrn exists in the searchFrequencies keys
        boolean found = false;
        for (String key : searchFrequencies.keySet()) {
            if (kmpSearch(key, ptrn)) {
                updateFrequency(key); // Update frequency if ptrn found
                found = true;
                ptrn = key; // Update ptrn to the matched key
                break;
            }
        }

        // If the ptrn was not found in any of the existing keys, add it as a new entry
        if (!found) {
            updateFrequency(ptrn);
        }

        saveFrequenciesToFile(); // Save the updated frequencies to the file

        // Return the frequency of the searched query
        return searchFrequencies.get(ptrn);
    }

    // Using KMP algorithm to search the ptrn
    private boolean kmpSearch(String text, String ptrn) {
        int[] lps = computeLPSArray(ptrn); // Compute the LPS array
        int i = 0, j = 0; // Pointers for text and ptrn
        while (i < text.length()) {
            if (ptrn.charAt(j) == text.charAt(i)) {
                i++;
                j++;
            }
            if (j == ptrn.length()) {
                return true; // ptrn found
            } else if (i < text.length() && ptrn.charAt(j) != text.charAt(i)) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        return false; // ptrn not found
    }

    // Method to find the longest prefix suffix
    private int[] computeLPSArray(String ptrn) {
        int[] lps = new int[ptrn.length()];
        int len = 0; // Length of last lps
        int i = 1;
        lps[0] = 0; // lps[0] is always 0
        while (i < ptrn.length()) {
            if (ptrn.charAt(i) == ptrn.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        return lps;
    }

    // Method to update the frequency of the input word in the HashMap
    private void updateFrequency(String word) {
        word = word.toLowerCase(); // Convert the word to lowercase
        searchFrequencies.put(word, searchFrequencies.getOrDefault(word, 0) + 1);
    }

    // Method for loading search frequencies from the file to the HashMap
    private void loadFrequenciesFromFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String word = parts[0].trim().toLowerCase(); // Convert to lowercase
                    int freq = Integer.parseInt(parts[1].trim());
                    searchFrequencies.put(word, freq);
                }
            }
        } catch (IOException e) {
            // File might not exist yet, which is fine.
        }
    }

    // Method to save the current search frequencies from the HashMap to the file
    public void saveFrequenciesToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Map.Entry<String, Integer> entry : searchFrequencies.entrySet()) {
                bw.write(entry.getKey() + ": " + entry.getValue());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Main method for testing purposes
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        SearchFrequencyTracker tracker = new SearchFrequencyTracker();

        // Ask user to search
        System.out.print("Enter the search query: ");
        String query = scanner.nextLine();

        // Execute search and get the frequency
        int frequency = tracker.search(query);

        // Print the frequency of the searched query
        System.out.println(query.toLowerCase() + " has been searched: " + frequency + " times.");

        // Close the scanner
        scanner.close();
    }
}
