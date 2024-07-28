import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
// Page ranking uses Boyer–Moore string-search algorithm and inverted indexing
public class PageRanking_BM {
    private Map<String, Map<String, Integer>> invertedIndex = new HashMap<>();
    private Map<String, String> propertyContent = new HashMap<>();
    private PriorityQueue<Map.Entry<String, Integer>> propertyMaxHeap;
    private String propertydataCSVFilePath;

    public PageRanking_BM(String propertydataCSVFilePath, String[] searchKeywords) {
        this.propertydataCSVFilePath = propertydataCSVFilePath;
        parsePropertyDataCSV(this.propertydataCSVFilePath);
        calculatePropertyRanks(searchKeywords);
    }

    private void parsePropertyDataCSV(String propertydataCSVFilePath) {
        try (FileReader fileReader = new FileReader(propertydataCSVFilePath);
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord csvRecord : csvParser) {
                StringBuilder fullContent = new StringBuilder();
                String propertyURL = "";

                for (String column : csvParser.getHeaderNames()) {
                    String value = csvRecord.get(column);
                    fullContent.append(value).append(" ");
                    if (column.equals("URL")) {
                        propertyURL = value;
                    }
                }

                String content = fullContent.toString().toLowerCase();
                propertyContent.put(propertyURL, content);

                String[] words = content.split("\\s+");
                for (String word : words) {
                    if (!word.isEmpty()) {
                        invertedIndex.computeIfAbsent(word, k -> new HashMap<>())
                                .merge(propertyURL, 1, Integer::sum);
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void calculatePropertyRanks(String[] searchKeywords) {
        Map<String, Integer> propertyRanks = new HashMap<>();

        for (String keyword : searchKeywords) {
            Map<String, Integer> keywordOccurrences = invertedIndex.getOrDefault(keyword.toLowerCase(), Collections.emptyMap());
            for (Map.Entry<String, Integer> entry : keywordOccurrences.entrySet()) {
                propertyRanks.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        propertyMaxHeap = new PriorityQueue<>(
                Comparator.comparingInt((Map.Entry<String, Integer> entry) -> entry.getValue()).reversed()
        );
        propertyMaxHeap.addAll(propertyRanks.entrySet());
    }

    public void print_TopRankedProperties(int num) {
        int count = 0;
        if (propertyMaxHeap.isEmpty()){
            System.out.println("No listings found with the specified keyword(s)");
            return;
        }
        System.out.println("Highest Ranked Properties:");
        while (!propertyMaxHeap.isEmpty() && count < num) {
            Map.Entry<String, Integer> entry = propertyMaxHeap.poll();
            System.out.println("--------------------");
            System.out.println("URL: " + entry.getKey() + ", Rank: " + entry.getValue());
            count++;
        }
    }
}