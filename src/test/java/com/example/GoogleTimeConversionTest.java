package com.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;

public class GoogleTimeConversionTest {
    private WebDriver driver;
    private WebDriverWait wait;
    private String screenshotDir;
    private boolean testFailed;

    @BeforeClass
    public void setUpClass() {
        // Setup WebDriverManager once for all tests
        WebDriverManager.chromedriver().setup();
        // Create screenshots directory if it doesn't exist
        screenshotDir = "screenshots";
        new File(screenshotDir).mkdirs();
    }

    @BeforeMethod
    public void setUp() {
        testFailed = false;
        try {
            // Configure Chrome options
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--start-maximized");
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-popup-blocking");

            // Initialize the ChromeDriver
            driver = new ChromeDriver(options);
            wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        } catch (Exception e) {
            System.err.println("Failed to initialize WebDriver: " + e.getMessage());
            throw e;
        }
    }

    private void takeScreenshot(String name) {
        if (driver == null) {
            System.err.println("Cannot take screenshot - WebDriver is null");
            return;
        }

        try {
            // Create timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            // Take screenshot
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            // Create file name
            String fileName = String.format("%s/%s_%s.png", screenshotDir, timestamp, name);

            // Save screenshot
            FileUtils.copyFile(screenshot, new File(fileName));
            System.out.println("Screenshot saved: " + fileName);
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
            // Don't throw the exception - we want to continue the test
        }
    }

    private void scrollAndHighlight(WebElement element) {
        if (driver == null || element == null)
            return;

        try {
            wait.until(ExpectedConditions.visibilityOf(element));

            // Scroll element into view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            Thread.sleep(500); // Small delay for scroll to complete

            // Highlight the element
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.border='3px solid red'", element);
        } catch (Exception e) {
            System.err.println("Error in scrollAndHighlight: " + e.getMessage());
        }
    }

    private void waitForPageLoad() {
        if (driver == null)
            return;

        try {
            wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState").equals("complete"));
            Thread.sleep(1000); // Short delay for any dynamic content
        } catch (Exception e) {
            System.err.println("Error while waiting for page load: " + e.getMessage());
        }
    }

    @Test
    public void testTimeConversion() {
        try {
            // Navigate to Google
            driver.get("https://www.google.com");
            waitForPageLoad();
            takeScreenshot("1_google_homepage");

            // Find and interact with the search box
            WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(By.name("q")));
            searchBox.clear();
            takeScreenshot("2_search_box_clear");

            searchBox.sendKeys("convert 3pm ist to est");
            takeScreenshot("3_search_text_entered");

            searchBox.submit();
            waitForPageLoad();
            takeScreenshot("4_search_submitted");

            // Wait for and find time conversion results using multiple strategies
            WebElement timeResult = findTimeConversionResult();

            // Verify and print the result
            if (timeResult != null) {
                scrollAndHighlight(timeResult);
                takeScreenshot("6_time_result_highlighted");

                String conversionResult = timeResult.getText();
                System.out.println("Time Conversion Result: " + conversionResult);
                Assert.assertFalse(conversionResult.isEmpty(), "Time conversion result should not be empty");
            } else {
                takeScreenshot("error_no_result_found");
                Assert.fail("Could not find time conversion result on the page");
            }

            // Take a final screenshot
            takeScreenshot("7_final_result");

        } catch (Exception e) {
            testFailed = true;
            takeScreenshot("error_" + e.getClass().getSimpleName());
            System.err.println("Test failed with exception: " + e.getMessage());
            throw e;
        }
    }

    private WebElement findTimeConversionResult() {
        // Array of possible selectors to try
        String[][] selectors = {
                { "div[data-tts='answers']", "css" },
                { ".card-section", "css" },
                { "div.gsrt", "css" },
                { "div.vk_bk", "css" },
                { "//*[contains(text(),'EST') or contains(text(),'Eastern Time')]", "xpath" },
                { "//div[contains(@class, 'card-section')]//div[contains(text(),'EST')]", "xpath" },
                { "//div[contains(@class, 'UQt4rd')]", "xpath" }
        };

        // Try each selector
        for (String[] selector : selectors) {
            try {
                System.out.println("Trying selector: " + selector[0]);
                WebElement element = null;

                if (selector[1].equals("css")) {
                    element = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector(selector[0])));
                } else {
                    element = wait.until(ExpectedConditions.presenceOfElementLocated(
                            By.xpath(selector[0])));
                }

                if (element != null && element.isDisplayed()) {
                    System.out.println("Found result with selector: " + selector[0]);
                    takeScreenshot("5_result_found_" + selector[1]);
                    return element;
                }
            } catch (Exception e) {
                System.out.println("Selector failed: " + selector[0] + " - " + e.getMessage());
            }
        }

        // If still not found, try to get all visible text elements
        try {
            System.out.println("Trying to find by scanning all visible text elements...");
            List<WebElement> allElements = driver.findElements(By.cssSelector("*"));
            for (WebElement element : allElements) {
                try {
                    if (element.isDisplayed()) {
                        String text = element.getText();
                        if (text.contains("EST") || text.contains("Eastern Time")) {
                            System.out.println("Found result by text content: " + text);
                            takeScreenshot("5_result_found_by_text");
                            return element;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            System.err.println("Error while scanning elements: " + e.getMessage());
        }

        return null;
    }

    @Test
    public void testTAUNavigation() {
        try {
            // Navigate to Test Automation University
            driver.get("https://testautomationu.applitools.com/");
            waitForPageLoad();
            takeScreenshot("1_tau_homepage");
            System.out.println("Navigated to TAU homepage");

            // Store the initial URL
            String initialUrl = driver.getCurrentUrl();

            // Wait for the page to load and find the Certificates link
            WebElement certificatesLink = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//a[contains(text(), 'Certificates') or contains(@href, 'certificates')]")));

            // Highlight the Certificates link and take screenshot before clicking
            scrollAndHighlight(certificatesLink);
            takeScreenshot("2_certificates_link_highlighted");

            // Click the Certificates link
            certificatesLink.click();
            waitForPageLoad();
            takeScreenshot("3_after_certificates_click");

            // Get and print the current URL
            String currentUrl = driver.getCurrentUrl();
            System.out.println("Certificates page URL: " + currentUrl);

            // Take final screenshot
            takeScreenshot("4_certificates_page");

            // Verify we're on a different URL than the homepage
            Assert.assertNotEquals(currentUrl, initialUrl,
                    "URL should change after clicking Certificates");

        } catch (Exception e) {
            testFailed = true;
            takeScreenshot("error_" + e.getClass().getSimpleName());
            System.err.println("Test failed with exception: " + e.getMessage());
            throw e;
        }
    }

    @AfterMethod
    public void tearDown() {
        try {
            if (driver != null) {
                if (testFailed) {
                    takeScreenshot("final_error_state");
                } else {
                    takeScreenshot("8_test_completion");
                }
                driver.quit();
                driver = null;
            }
        } catch (Exception e) {
            System.err.println("Error in tearDown: " + e.getMessage());
        }
    }

    @AfterClass
    public void tearDownClass() {
        // Cleanup any remaining resources
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Error in final cleanup: " + e.getMessage());
            }
            driver = null;
        }
    }
}