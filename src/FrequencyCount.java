import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class FrequencyCount {

    // Method to parse the CSV file and update word frequencies in the map
    public static void parseCSV(String filePath, Map<String, Integer> cityWordCountMap, Map<String, List<String[]>> cityListingsMap, Map<String, Integer> provinceWordCountMap, Map<String, List<String[]>> provinceListingsMap) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Skip the header line
            while ((line = br.readLine()) != null) {
                String[] columns = parseCSVLine(line);
                if (columns.length >= 6) {
                    String city = columns[2].toLowerCase();
                    String province = columns[3].toUpperCase();

                    cityWordCountMap.put(city, cityWordCountMap.getOrDefault(city, 0) + 1);
                    cityListingsMap.computeIfAbsent(city, k -> new ArrayList<>()).add(columns);

                    provinceWordCountMap.put(province, provinceWordCountMap.getOrDefault(province, 0) + 1);
                    provinceListingsMap.computeIfAbsent(province, k -> new ArrayList<>()).add(columns);
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle IO exception
        }
    }

    // Method to parse a single CSV line
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
                        columns.add(sb.toString().trim());
                        sb.setLength(0);
                    }
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }
        columns.add(sb.toString().trim());
        return columns.toArray(new String[0]);
    }

    // Method to display the frequency of a specific word
    public static void displayWordFrequency(Map<String, Integer> wordCountMap, String word, boolean isProvince) {
        word = isProvince ? word.toUpperCase() : word.toLowerCase(); // Convert word to uppercase for province, lowercase for city
        int count = wordCountMap.getOrDefault(word, 0); // Looks up the word in the correct case
        System.out.println("\033[38;5;208mTotal listings for " + word + ": " + count + "\033[0m");
    }

    // Method to search word frequency in a URL
    public static void searchWordFrequencyInUrl(String urlString, String wordToCount) {
        Scanner scanner = new Scanner(System.in);
        while (!isValidUrl(urlString)) {
            System.out.println("Invalid URL format. Please enter a valid URL:");
            urlString = scanner.nextLine();
        }
        try {
            String content = fetchContentFromUrl(urlString);
            int frequency = boyerMooreSearch(content.toLowerCase(), wordToCount.toLowerCase());
            System.out.println("The word '" + wordToCount + "' appears " + frequency + " times in the URL.");
        } catch (IOException e) {
            System.out.println("An error occurred while fetching the URL content: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("An error occurred: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
        }
    }


    // Fetch content from a URL with timeout
    private static String fetchContentFromUrl(String urlString) throws IOException {
        StringBuilder content = new StringBuilder();
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        connection.setConnectTimeout(5000); // 5 seconds timeout for connection
        connection.setReadTimeout(5000); // 5 seconds timeout for reading data

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to fetch URL content. HTTP response code: " + responseCode);
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine).append("\n");
            }
        }
        return content.toString();
    }

    // Boyer-Moore algorithm for counting occurrences of a word in a text
    private static int boyerMooreSearch(String text, String pattern) {
        if (pattern.length() == 0) {
            throw new IllegalArgumentException("Pattern must not be empty");
        }

        Map<Character, Integer> badCharTable = buildBadCharTable(pattern);
        int count = 0;
        int m = pattern.length();
        int n = text.length();

        int s = 0; // s is the shift of the pattern with respect to text
        while (s <= (n - m)) {
            int j = m - 1;

            // Keep reducing index j of pattern while characters of pattern and text are matching at this shift s
            while (j >= 0 && pattern.charAt(j) == text.charAt(s + j))
                j--;

            // If the pattern is present at current shift, then index j will become -1 after the above loop
            if (j < 0) {
                count++;
                s += (s + m < n) ? m - badCharTable.getOrDefault(text.charAt(s + m), -1) : 1;
            } else {
                s += Math.max(1, j - badCharTable.getOrDefault(text.charAt(s + j), -1));
            }
        }
        return count;
    }

    // Preprocesses the pattern to create the bad character table
    private static Map<Character, Integer> buildBadCharTable(String pattern) {
        Map<Character, Integer> badCharTable = new HashMap<>();
        int m = pattern.length();

        // Initialize all occurrences as -1
        for (int i = 0; i < m; i++)
            badCharTable.put(pattern.charAt(i), i);

        return badCharTable;
    }

    // Method to analyze and rank URLs from CSV
    public static void analyzeAndRankUrlsFromCsv(String csvFilePath, String wordToCount) {
        // Ensure valid word input from user
        final String validWordToCount = getValidWordInput(wordToCount);

        // Check if the results are already cached
        String cacheFilePath = "stored_" + validWordToCount + ".txt";
        File cacheFile = new File(cacheFilePath);
        if (cacheFile.exists()) {
            displayCachedResults(cacheFilePath);
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<PropertyRank>> futures = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            br.readLine(); // Skip the header line

            while ((line = br.readLine()) != null) {
                String[] columns = parseCSVLine(line);
                if (columns.length >= 6) {
                    String url = columns[5];
                    futures.add(executor.submit(() -> {
                        int frequency = searchWordFrequencyInUrlForRank(url, validWordToCount);
                        return new PropertyRank(columns, frequency);
                    }));
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle IO exception
        }

        List<PropertyRank> rankedProperties = new ArrayList<>();
        for (Future<PropertyRank> future : futures) {
            try {
                rankedProperties.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                // Suppress error messages
                // e.printStackTrace();
            }
        }

        executor.shutdown();

        rankedProperties.sort(Comparator.comparingInt(PropertyRank::getFrequency).reversed());
        cacheResults(rankedProperties, cacheFilePath);

        System.out.println("\033[38;5;129mDisplaying the top 10 listings based on frequency:\033[0m");

        for (int i = 0; i < Math.min(10, rankedProperties.size()); i++) {
            PropertyRank property = rankedProperties.get(i);
            displayProperty(property);
        }
    }

    // Method to get valid word input
    private static String getValidWordInput(String wordToCount) {
        Scanner scanner = new Scanner(System.in);
        while (!isValidWord(wordToCount)) {
            System.out.println("The word to count must only contain alphabetic characters. Please enter again:");
            wordToCount = scanner.nextLine();
        }
        return wordToCount;
    }

    private static int searchWordFrequencyInUrlForRank(String urlString, String wordToCount) {
        if (!isValidUrl(urlString)) {
            return 0;
        }
        try {
            String content = fetchContentFromUrl(urlString);
            return boyerMooreSearch(content.toLowerCase(), wordToCount.toLowerCase());
        } catch (IOException e) {
            // Suppress error messages
            // System.out.println("An error occurred while fetching the URL content: " + e.getMessage());
            return 0;
        }
    }

    private static void cacheResults(List<PropertyRank> rankedProperties, String cacheFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFilePath))) {
            for (PropertyRank property : rankedProperties) {
                writer.write(Arrays.toString(property.getColumns()) + "\t" + property.getFrequency());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void displayCachedResults(String cacheFilePath) {
        List<PropertyRank> rankedProperties = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(cacheFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                String[] columns = parts[0].replaceAll("[\\[\\]]", "").split(", ");
                int frequency = Integer.parseInt(parts[1]);

                rankedProperties.add(new PropertyRank(columns, frequency));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Sort the properties based on frequency in descending order
        rankedProperties.sort(Comparator.comparingInt(PropertyRank::getFrequency).reversed());

        System.out.println("\033[38;5;129mDisplaying the top 10 listings based on frequency:\033[0m");

        // Display top 10 results
        for (int i = 0; i < Math.min(10, rankedProperties.size()); i++) {
            PropertyRank property = rankedProperties.get(i);
            displayProperty(property);
        }
    }

    private static void displayProperty(PropertyRank property) {
        String[] columns = property.getColumns();
        int frequency = property.getFrequency();

        System.out.println("Price: " + columns[0]);
        System.out.println("Address: " + columns[1]);
        System.out.println("City: " + columns[2]);
        System.out.println("Province: " + columns[3]);
        System.out.println("Details: " + columns[4]);
        System.out.println("URL: " + columns[5]);
        System.out.println("Image: " + columns[6]);
        System.out.println("Frequency: " + frequency);
        System.out.println();
    }

    // Method to validate the word input
    private static boolean isValidWord(String word) {
        return word != null && word.chars().allMatch(Character::isAlphabetic);
    }

    // Method to validate the URL format using regex
    private static boolean isValidUrl(String urlString) {
        String regex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(urlString);
        return matcher.matches();
    }

    private static class PropertyRank {
        private final String[] columns;
        private final int frequency;

        public PropertyRank(String[] columns, int frequency) {
            this.columns = columns;
            this.frequency = frequency;
        }

        public String[] getColumns() {
            return columns;
        }

        public int getFrequency() {
            return frequency;
        }
    }}
