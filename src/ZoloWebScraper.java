import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.NoSuchElementException;
import java.util.logging.*;

// Zolo web scraper class
public class ZoloWebScraper {
    private static final Logger logger = Logger.getLogger(ZoloWebScraper.class.getName());
    private static final int WAIT_TIMEOUT = 30; // Increased from 10 to 30 seconds
    private static final String BASE_URL = "https://www.zolo.ca/";
    private static final int MAX_PAGES = 10;
    private static final int MAX_RETRIES = 3;
    private static final Random random = new Random();

    private String[] locations = {
            "Yukon", "Windsor, ON", "Toronto", "Ottawa", "Victoria", "Edmonton", "Regina",
            "Winnipeg", "St. John", "Charlottetown", "Halifax", "Fredericton", "Calgary"
    };

    private String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Safari/605.1.15",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Safari/537.36"
    };

    public ZoloWebScraper() {
        setupLogger();
    }

    private void setupLogger() {
        try {
            FileHandler fileHandler = new FileHandler("zolo_scraper.log");
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void scrapeAllLocations() {
        WebDriver driver = null;
        try {
            driver = setupWebDriver();
            List<Map<String, String>> allListings = new ArrayList<>();

            for (String location : locations) {
                logger.info("Scraping location: " + location);
                List<Map<String, String>> locationListings = scrapeListings(driver, location);
                allListings.addAll(locationListings);
            }

            writeToCSV(allListings);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error occurred during scraping", e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private WebDriver setupWebDriver() {
        System.setProperty("webdriver.chrome.driver", "/opt/homebrew/bin/chromedriver");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--window-size=1920,1200",
                "--ignore-certificate-errors", "--disable-extensions",
                "--no-sandbox", "--disable-dev-shm-usage");
        options.addArguments("user-agent=" + getRandomUserAgent());
        return new ChromeDriver(options);
    }

    private List<Map<String, String>> scrapeListings(WebDriver driver, String location) {
        List<Map<String, String>> listings = new ArrayList<>();
        int retries = 0;
        boolean success = false;

        while (retries < MAX_RETRIES && !success) {
            try {
                driver.get(BASE_URL);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT));

                WebElement searchInput = wait.until(ExpectedConditions.elementToBeClickable(By.id("sarea")));
                searchInput.clear();
                searchInput.sendKeys(location);
                randomDelay(1000, 2000);
                searchInput.sendKeys(Keys.ENTER);

                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".listings-wrapper")));

                int pageCount = 1;
                while (pageCount <= MAX_PAGES) {
                    logger.info("Scraping " + location + " - page " + pageCount);
                    WebElement listingsWrapper = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".listings-wrapper")));
                    List<WebElement> listingElements = listingsWrapper.findElements(By.cssSelector(".card-listing"));

                    for (WebElement listing : listingElements) {
                        try {
                            Map<String, String> listingData = extractListingData(listing);
                            if (listingData != null) {
                                listings.add(listingData);
                            }
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Error extracting listing data", e);
                        }
                    }

                    if (pageCount == MAX_PAGES || !goToNextPage(driver)) {
                        break;
                    }

                    pageCount++;
                    randomDelay(3000, 5000);
                }

                success = true;
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error scraping " + location + ". Retry " + (retries + 1), e);
                retries++;
                if (retries < MAX_RETRIES) {
                    randomDelay(5000, 10000);
                }
            }
        }

        if (!success) {
            logger.log(Level.SEVERE, "Failed to scrape " + location + " after " + MAX_RETRIES + " attempts");
        }

        return listings;
    }

    private void randomDelay(int minMillis, int maxMillis) {
        try {
            Thread.sleep(random.nextInt(maxMillis - minMillis) + minMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getRandomUserAgent() {
        return userAgents[random.nextInt(userAgents.length)];
    }

    private Map<String, String> extractListingData(WebElement listing) {
        String price = getElementTextSafely(listing, By.cssSelector("ul.card-listing--values > li:first-child"));

        // Skip listings with empty prices or prices containing 'X'
        if (price.isEmpty() || price.toLowerCase().contains("x")) {
            return null;
        }

        Map<String, String> data = new HashMap<>();
        data.put("Price", price);

        String fullAddress = getElementTextSafely(listing, By.cssSelector(".card-listing--location .address"));
        String[] addressParts = fullAddress.split(",");

        if (addressParts[0].equals("Ask us for address")) {
            return null;
        }

        data.put("Address", addressParts.length > 0 ? addressParts[0].trim() : "");

        data.put("City", addressParts.length > 1 ? addressParts[1].trim() : "");
        data.put("Province", addressParts.length > 2 ? addressParts[2].trim() : "");

        String beds = getElementTextSafely(listing, By.cssSelector("ul.card-listing--values > li:nth-child(2)"));
        String baths = getElementTextSafely(listing, By.cssSelector("ul.card-listing--values > li:nth-child(3)"));
        data.put("Details", beds + " " + baths);

        data.put("URL", getElementAttributeSafely(listing, By.cssSelector("a.card-listing--image-link"), "href"));

        String imageUrl = getElementAttributeSafely(listing, By.cssSelector("a.card-listing--image-link img"), "src");
        data.put("Image File", saveImage(imageUrl, data.get("Address")+"__"+data.get("City")+"__"+data.get("Province")));

        return data;
    }

    private boolean goToNextPage(WebDriver driver) {
        try {
            WebElement nextButton = waitForElement(driver, By.cssSelector("a[aria-label='next page of results']"));
            if (nextButton.isEnabled() && nextButton.isDisplayed()) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", nextButton);
                nextButton.click();
                waitForPageLoad(driver);
                return true;
            }
        } catch (TimeoutException e) {
            logger.info("No more pages available");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to navigate to next page", e);
        }
        return false;
    }

    private WebElement waitForElement(WebDriver driver, By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT));
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    private void waitForPageLoad(WebDriver driver) {
        new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT)).until(
                webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
        try {
            Thread.sleep(3000); // 3-second delay between page loads
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Sleep interrupted", e);
        }
    }


    private String getElementTextSafely(WebElement parent, By selector) {
        try {
            return parent.findElement(selector).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String getElementAttributeSafely(WebElement parent, By selector, String attribute) {
        try {
            return parent.findElement(selector).getAttribute(attribute);
        } catch (Exception e) {
            return "";
        }
    }

    private void writeToCSV(List<Map<String, String>> listings) {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter("zolo_listings.csv"),
                CSVFormat.DEFAULT.withHeader("Price", "Address", "City", "Province", "Details", "URL", "Image File"))) {
            for (Map<String, String> listing : listings) {
                printer.printRecord(
                        listing.get("Price"),
                        listing.get("Address")+",",
                        listing.get("City"),
                        listing.get("Province"),
                        listing.get("Details"),
                        listing.get("URL"),
                        listing.get("Image File")
                );
            }
            logger.info("Data extraction complete. Check zolo_listings.csv for results.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error writing to CSV", e);
        }
    }

    private String saveImage(String imageUrl, String address) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "No Image Available";
        }
        String imageFileName = address.replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
        try {
            FileUtils.copyURLToFile(new URL(imageUrl), new File("images/" + imageFileName));
            return "images/" + imageFileName;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save image", e);
            return "Failed to save image";
        }
    }

    public static void main(String[] args) {
        ZoloWebScraper scraper = new ZoloWebScraper();
        scraper.scrapeAllLocations();
    }
}