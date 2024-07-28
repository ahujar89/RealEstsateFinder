import java.io.*;
import java.util.*;
import com.opencsv.*;
import com.opencsv.exceptions.CsvException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class CSVMerger {
    private static final Logger logger = Logger.getLogger(CSVMerger.class.getName());

    public void appendZoloToRemax(String remaxFile, String zoloFile) throws IOException, CsvException {
        Set<String> uniqueListings = new HashSet<>();
        int remaxCount = 0;
        int zoloAdded = 0;
        int zoloDuplicates = 0;

        // Read Remax listings to get unique keys
        try (CSVReader reader = new CSVReader(new FileReader(remaxFile))) {
            List<String[]> remaxListings = reader.readAll();

            if (remaxListings.isEmpty()) {
                logger.warning("Remax file is empty");
                return;
            }

            // Add Remax listings to the set
            for (int i = 1; i < remaxListings.size(); i++) { // Skip header
                String[] listing = remaxListings.get(i);
                uniqueListings.add(getUniqueKey(listing));
                remaxCount++;
            }
        }

        logger.info("Read " + remaxCount + " Remax listings");

        // Append unique Zolo listings to Remax file
        try (CSVReader reader = new CSVReader(new FileReader(zoloFile));
             CSVWriter writer = new CSVWriter(new FileWriter(remaxFile, true))) {

            List<String[]> zoloListings = reader.readAll();

            if (zoloListings.isEmpty()) {
                logger.warning("Zolo file is empty");
                return;
            }

            // Skip header for Zolo listings
            for (int i = 1; i < zoloListings.size(); i++) {
                String[] listing = zoloListings.get(i);
                String key = getUniqueKey(listing);
                if (!uniqueListings.contains(key)) {
                    writer.writeNext(listing);
                    uniqueListings.add(key);
                    zoloAdded++;
                } else {
                    zoloDuplicates++;
                }
            }
        }

        logger.info("Zolo listings appended to " + remaxFile + " successfully.");
        logger.info("Zolo listings added: " + zoloAdded);
        logger.info("Zolo duplicates skipped: " + zoloDuplicates);
    }

    private String getUniqueKey(String[] listing) {
        // Assuming the format is: Price,Address,City,Province,Details,URL,Image File
        return listing[1].trim() + "|" + listing[2].trim() + "|" + listing[3].trim(); // Address|City|Province
    }

    // This method can be called from your main function
    public void runAppendProcess(String remaxFile, String zoloFile) {
        try {
            logger.info("Starting append process...");
            appendZoloToRemax(remaxFile, zoloFile);
            logger.info("Append process completed successfully");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO error during append process", e);
            throw new RuntimeException("IO error during append process", e);
        } catch (CsvException e) {
            logger.log(Level.SEVERE, "CSV parsing error during append process", e);
            throw new RuntimeException("CSV parsing error during append process", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error during append process", e);
            throw new RuntimeException("Unexpected error during append process", e);
        }
    }
}