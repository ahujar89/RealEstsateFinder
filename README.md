# Real Estsate Finder

Welcome to the Real Estate Scraper project! This application scrapes data from real estate websites like Remax and Zolo, processes the data, and provides users with various search functionalities to explore property listings. I have used Selenium to scrap the required data from Remax and Zolo websites.

## Features

- **Update Listings**: Scrape and update property listings from Remax and Zolo.
- **Explore Properties by Province**: Search and display property listings by province.
- **Discover Homes by City**: Search and display property listings by city.
- **Find Properties Within Budget**: Filter listings based on your budget.
- **Search Cozy Homes by Bedrooms and Bathrooms**: Filter listings based on the number of bedrooms and bathrooms.
- **City Name Autocomplete**: Get city suggestions based on the entered prefix.
- **Keyword-Based Search**: Hunt for properties using specific keywords.
- **Word Frequency Search in URLs**: Search for word frequency in a given URL.
- **Search by Property Type or Keyword**: Analyze and rank URLs from the CSV based on a keyword.

## Getting Started

### Prerequisites

- Java 8 or above
- Selenium
- Internet connection (for web scraping)

## Usage

### Main Menu Options

1. **Update listings with the magic scraper**: Scrape the latest listings from Remax and Zolo and update the CSV files. Note: This process can take around 30 minutes.
2. **Explore properties by Province**: Enter the province code to view property listings in that province.
3. **Discover homes by City**: Enter the city name to view property listings in that city.
4. **Find your dream property within your Budget**: Enter your budget to filter and view property listings within that range.
5. **Search cozy homes by bedrooms and bathrooms**: Filter property listings based on the number of bedrooms and bathrooms.
6. **City Name Autocomplete**: Enter a prefix to get city suggestions.
7. **Hunt for properties using keywords**: Enter keywords to search for properties and rank them based on relevance.
8. **Search word frequency in a URL**: Enter a URL and a word to count its frequency in that URL.
9. **Search by property type or keyword**: Enter a keyword to analyze and rank URLs from the CSV based on that keyword.
10. **Exit**: Close the application.

### Examples

#### Explore Properties by Province

1. Select option `2` from the main menu.
2. Enter the province code (e.g., `ON` for Ontario).
3. View the listings for the selected province.

#### Discover Homes by City

1. Select option `3` from the main menu.
2. Enter the city name (e.g., `Toronto`).
3. View the listings for the selected city.

#### Find Properties Within Budget

1. Select option `4` from the main menu.
2. Enter your budget (e.g., `300000`).
3. View the filtered listings within your budget.

## File Structure

- `Main.java`: The main class containing the application logic and user interface.
- `Autocomplete.java`: Handles city name autocomplete functionality.
- `CSVMerger.java`: Merges CSV files from different sources.
- `DictionaryGenerator.java`: Generates a dictionary file from the CSV for the spell checker.
- `FilterByPrice.java`: Filters listings based on the budget.
- `FilterByBedBath.java`: Filters listings based on the number of bedrooms and bathrooms.
- `FrequencyCount.java`: Counts word frequency in URLs and CSV.
- `PageRanking_BM.java`: Handles page ranking based on keywords.
- `RemaxWebScraper.java`: Scrapes data from Remax.
- `SearchFrequencyTracker.java`: Tracks search frequency for cities and provinces.
- `SpellChecker.java`: Handles spell checking functionality.
- `TorontoDataCleaner.java`: Cleans the data specific to Toronto.
- `ZoloWebScraper.java`: Scrapes data from Zolo.

## Acknowledgements

- [Remax](https://www.remax.ca/)
- [Zolo](https://www.zolo.ca/)

---

Happy property hunting! 🏡
