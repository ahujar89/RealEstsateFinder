import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.apache.commons.io.FileUtils;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

// Remax web scraper

public class RemaxWebScraper {

    private static final Logger logger = Logger.getLogger(RemaxWebScraper.class.getName());
    private String[] locations = {
            "Ontario", "Yukon", "Northwest Territories", "British Columbia", "Alberta", "Saskatchewan",
            "Manitoba", "Quebec", "Newfoundland and Labrador",
            "Windsor", "Toronto", "Ottawa", "Victoria", "Edmonton", "Regina",
            "Winnipeg", "St. John", "Charlottetown", "Halifax", "Fredericton", "Calgary"
    };
    // Remove the outputDir variable
    private static final int MAX_PAGES = 10;
    private static final int WAIT_TIMEOUT = 120;
    private static final int MAX_RETRIES = 10;
    private static final int RETRY_DELAY = 4000; // milliseconds
    private static final int PAGE_LOAD_WAIT = 10000; // milliseconds

    private Set<String> globalProcessedAddresses = new HashSet<>();

    public RemaxWebScraper() {
        setupLogger();
        // Remove the createOutputDirectory() method call
    }

    private void setupLogger() {
        try {
            FileHandler fileHandler = new FileHandler("scraper.log");
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void scrape(String locationName, CSVPrinter printer) {
        WebDriver driver = null;
        try {
            driver = setupWebDriver();
            navigateToRemaxAndSearch(driver, locationName);
            int pageCount = 0;
            while (pageCount < MAX_PAGES) {
                if (!processCurrentPage(driver, locationName, printer, pageCount)) {
                    break;
                }
                pageCount++;
                if (!goToNextPage(driver)) {
                    logger.info("No more pages available for " + locationName);
                    break;
                }
            }
            logger.info("Data extraction complete for location: " + locationName);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error occurred while scraping " + locationName, e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    private WebDriver setupWebDriver() {
        System.setProperty("webdriver.chrome.driver", "/opt/homebrew/bin/chromedriver");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        return new ChromeDriver(options);
    }

    private void navigateToRemaxAndSearch(WebDriver driver, String locationName) {
        driver.get("https://www.remax.ca/");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT));
        logger.info("Navigated to RE/MAX homepage");

        WebElement searchBox = waitForElement(driver, By.id("home-search-input"));
        searchBox.sendKeys(locationName);
        logger.info("Entered location name: " + locationName);

        try {
            WebElement firstSuggestion = waitForElement(driver, By.id("home-search-item-1"));
            firstSuggestion.click();
            logger.info("Clicked on first suggestion for: " + locationName);
        } catch (TimeoutException e) {
            logger.warning("No suggestions found for " + locationName + ". Attempting to search directly.");
            searchBox.sendKeys(Keys.ENTER);
        }

        try {
            waitForElement(driver, By.cssSelector(".search-gallery_galleryContainer__k32f5"));
            logger.info("Search results loaded for: " + locationName);
        } catch (TimeoutException e) {
            logger.warning("No search results found for " + locationName);
        }
    }

    private boolean processCurrentPage(WebDriver driver, String locationName, CSVPrinter printer, int pageCount) throws IOException {
        scrollToLoadAllListings(driver);
        List<WebElement> propertyListings = getPropertyListings(driver);
        logger.info("Processing page " + (pageCount + 1) + " for " + locationName + ". Found " + propertyListings.size() + " listings.");

        if (propertyListings.isEmpty()) {
            logger.warning("No listings found on page " + (pageCount + 1) + " for " + locationName);
            return false;
        }

        int successfullyProcessed = 0;
        for (int i = 0; i < propertyListings.size(); i++) {
            try {
                WebElement property = propertyListings.get(i);
                if (processProperty(driver, property, printer, locationName)) {
                    successfullyProcessed++;
                }
            } catch (StaleElementReferenceException e) {
                logger.warning("Stale element encountered. Refreshing property listings.");
                propertyListings = refreshPropertyListings(driver);
                i--; // Retry the same index
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to process a property on page " + (pageCount + 1) + " for " + locationName, e);
            }
        }

        logger.info("Successfully processed " + successfullyProcessed + " out of " + propertyListings.size() + " listings on page " + (pageCount + 1));
        return true;
    }

    private List<WebElement> getPropertyListings(WebDriver driver) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                List<WebElement> listings = driver.findElements(By.cssSelector(".listing-card_root__RBrXm"));
                if (!listings.isEmpty()) {
                    return listings;
                }
            } catch (StaleElementReferenceException e) {
                logger.info("StaleElementReferenceException caught while getting property listings. Retrying...");
            }
            sleep(RETRY_DELAY);
        }
        logger.warning("Failed to get property listings after " + MAX_RETRIES + " attempts");
        return new ArrayList<>();
    }

    private List<WebElement> refreshPropertyListings(WebDriver driver) {
        sleep(PAGE_LOAD_WAIT);
        return getPropertyListings(driver);
    }

    private boolean processProperty(WebDriver driver, WebElement property, CSVPrinter printer, String locationName) throws IOException {
        String price = getElementTextSafely(driver, property, By.cssSelector(".listing-card_price__lEBmo"));

        // Extract full address including city and province
        WebElement addressElement = waitForElement(driver, property, By.cssSelector("[data-cy='property-address']"));
        String streetAddress = getElementTextSafely(driver, addressElement, By.cssSelector("span:first-child"));
        String cityProvince = getElementTextSafely(driver, addressElement, By.cssSelector("span:last-child"));

        // Combine all parts into a full address
        String fullAddress = (streetAddress + " " + cityProvince).trim();

        if (fullAddress.isEmpty()) {
            logger.warning("Empty address found. Skipping property.");
            return false;
        }

        // Check against global set of processed addresses using the full address
        if (globalProcessedAddresses.contains(fullAddress)) {
            logger.info("Skipping duplicate address: " + fullAddress);
            return false;
        }
        globalProcessedAddresses.add(fullAddress);

        // Parse city and province
        String[] cityProvinceparts = cityProvince.split(", ");
        String city = cityProvinceparts.length > 0 ? cityProvinceparts[0] : "";
        String province = cityProvinceparts.length > 1 ? cityProvinceparts[1] : "";

        // If the location is a province, use it as the province
        if (isProvince(locationName) && province.isEmpty()) {
            province = locationName;
        }

        WebElement detailsElement = waitForElement(driver, property, By.cssSelector(".listing-card_detailsRow__t1YUs"));
        String beds = getElementTextSafely(driver, detailsElement, By.cssSelector("[data-cy='property-beds']"));
        String baths = getElementTextSafely(driver, detailsElement, By.cssSelector("[data-cy='property-baths']"));
        String details = beds + " " + baths;

        String url = getElementAttributeSafely(driver, property, By.cssSelector(".listing-card_listingCard__lc4CL"), "href");
        String imageUrl = getImageUrl(driver, property);
        String imageFileName = saveImage(imageUrl, fullAddress);

        printer.printRecord(price, streetAddress, city, province, details, url, imageFileName);
        logger.info("Processed property: " + fullAddress);
        return true;
    }

    private boolean goToNextPage(WebDriver driver) {
        try {
            WebElement nextButton = waitForElement(driver, By.cssSelector(".page-control_arrowButtonRoot__GNsT1[aria-label='Go to the next page of the gallery.']"));
            if (nextButton.isEnabled() && !nextButton.getAttribute("class").contains("Mui-disabled")) {
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
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private WebElement waitForElement(WebDriver driver, WebElement parent, By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT));
        return wait.until(ExpectedConditions.visibilityOf(parent.findElement(locator)));
    }

    private String getElementTextSafely(WebDriver driver, WebElement parent, By locator) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return waitForElement(driver, parent, locator).getText();
            } catch (StaleElementReferenceException e) {
                if (attempt == MAX_RETRIES - 1) {
                    logger.log(Level.WARNING, "Failed to get element text after " + MAX_RETRIES + " attempts", e);
                    return "";
                }
            }
            sleep(RETRY_DELAY);
        }
        return "";
    }

    private String getElementAttributeSafely(WebDriver driver, WebElement parent, By locator, String attribute) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return waitForElement(driver, parent, locator).getAttribute(attribute);
            } catch (StaleElementReferenceException e) {
                if (attempt == MAX_RETRIES - 1) {
                    logger.log(Level.WARNING, "Failed to get element attribute after " + MAX_RETRIES + " attempts", e);
                    return "";
                }
            }
            sleep(RETRY_DELAY);
        }
        return "";
    }

    private String getImageUrl(WebDriver driver, WebElement property) {
        try {
            WebElement imageElement = property.findElement(By.cssSelector("img.lazyloaded, img.lazyload, img.image_blurUp__uxKUD"));
            return imageElement.getAttribute("src");
        } catch (Exception e) {
            logger.log(Level.WARNING, "No image found for listing", e);
            return "";
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

    private void scrollToLoadAllListings(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long lastHeight = (long) js.executeScript("return document.body.scrollHeight");

        while (true) {
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            sleep(2000);

            long newHeight = (long) js.executeScript("return document.body.scrollHeight");
            if (newHeight == lastHeight) {
                break;
            }
            lastHeight = newHeight;
        }
    }

    private void waitForPageLoad(WebDriver driver) {
        new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT)).until(
                webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
        sleep(PAGE_LOAD_WAIT);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Sleep interrupted", e);
        }
    }

    private boolean isProvince(String location) {
        String[] provinces = {"Ontario", "Yukon", "Northwest Territories", "British Columbia", "Alberta", "Saskatchewan",
                "Manitoba", "Quebec", "Newfoundland and Labrador", "Prince Edward Island", "Nova Scotia", "New Brunswick"};
        for (String province : provinces) {
            if (province.equalsIgnoreCase(location)) {
                return true;
            }
        }
        return false;
    }

    public void scrapeMultipleLocations() {
        try (FileWriter csvWriter = new FileWriter("remax_listings.csv");
             CSVPrinter printer = new CSVPrinter(csvWriter, CSVFormat.DEFAULT.withHeader("Price", "Address", "City", "Province", "Details", "URL", "Image File"))) {
            for (String location : locations) {
                scrape(location, printer);
            }
            logger.info("Data extraction complete. Check remax_listings.csv for results.");
            logger.info("Total unique properties processed: " + globalProcessedAddresses.size());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to scrape data for locations", e);
        }
    }
}