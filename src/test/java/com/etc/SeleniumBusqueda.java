package com.etc;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SeleniumBusqueda {
    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeTest
    public void setUp() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("intl.accept_languages", "en,en_US");
        prefs.put("translate.enabled", false);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--lang=en");
        options.addArguments("--accept-lang=en-US");

        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        ((JavascriptExecutor) driver)
                .executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
    }

    @Test
    public void testSeleniumDocumentationNavigation() throws IOException, InterruptedException {

        driver.get("https://www.google.com/?hl=en");
        acceptGoogleCookiesIfPresent();
        shortPause();

        By searchLocator = By.cssSelector("textarea[name='q'], input[name='q']");
        WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(searchLocator));
        searchBox.click();
        searchBox.sendKeys("Documentación de selenium");

        takeScreenshot("1_busqueda_google_escribiendo.png", searchLocator);

        searchBox.sendKeys(Keys.ENTER);

        By resultsContainer = By.cssSelector("#search");
        wait.until(ExpectedConditions.presenceOfElementLocated(resultsContainer));

        By firstResultTitle = By.cssSelector("#search h3");
        wait.until(ExpectedConditions.presenceOfElementLocated(firstResultTitle));
        takeScreenshot("2_resultados_google.png", firstResultTitle);

        driver.navigate().to("https://www.selenium.dev/documentation/");
        wait.until(ExpectedConditions.urlContains("selenium.dev"));
        shortPause();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));
        takeScreenshot("3_documentacion_selenium_home.png", By.tagName("h1"));

        MenuItem[] menuItems = new MenuItem[]{
                new MenuItem("overview", "/documentation/overview/"),
                new MenuItem("webdriver", "/documentation/webdriver/"),
                new MenuItem("selenium_manager", "/documentation/selenium_manager/"),
                new MenuItem("grid", "/documentation/grid/"),
                new MenuItem("ie_driver_server", "/documentation/ie_driver_server/"),
                new MenuItem("ide", "/documentation/ide/"),
                new MenuItem("test_practices", "/documentation/test_practices/"),
                new MenuItem("legacy", "/documentation/legacy/"),
                new MenuItem("about", "/about/")
        };

        for (MenuItem item : menuItems) {
            try {
                if (driver.getCurrentUrl().contains("translate.goog")) {
                    driver.navigate().to("https://www.selenium.dev/documentation/");
                    wait.until(ExpectedConditions.urlContains("selenium.dev"));
                    shortPause();
                }

                // ESTE ES EL CAMBIO: para Legacy y About usamos el menú IZQUIERDO, para click y para rectángulo.
                By clickLocator;
                By highlightLocator;

                if ("legacy".equals(item.name)) {
                    clickLocator = By.xpath("//aside//*[self::a or self::span][normalize-space()='Legacy']");
                    highlightLocator = clickLocator;
                } else if ("about".equals(item.name)) {
                    clickLocator = By.xpath("//aside//*[self::a or self::span][normalize-space()='About']");
                    highlightLocator = clickLocator;
                } else {
                    clickLocator = By.xpath("//a[@href='" + item.href + "']");
                    highlightLocator = clickLocator;
                }

                // Click (About y Legacy ahora hacen click en el panel izquierdo)
                clickElementWithRetry(clickLocator);
                shortPause();

                // Asegura que el item del menú izquierdo quede visible para la captura
                WebElement menuLink = wait.until(ExpectedConditions.presenceOfElementLocated(highlightLocator));
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({block:'center'});", menuLink
                );
                shortPause();

                // Screenshot con rectángulo sobre el item correcto del menú izquierdo
                if ("about".equals(item.name)) {
                    takeScreenshot("menu_about_visible.png", highlightLocator);
                } else {
                    takeScreenshot("menu_" + item.name + ".png", highlightLocator);
                    wait.until(ExpectedConditions.urlContains(item.href));
                }

            } catch (TimeoutException e) {
                System.out.println("No se pudo encontrar o hacer clic en: " + item.href);
            } catch (RuntimeException e) {
                System.out.println("Error al hacer clic en: " + item.href + " -> " + e.getMessage());
            }
        }
    }

    @AfterTest
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    private void acceptGoogleCookiesIfPresent() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));

            List<By> buttons = List.of(
                    By.id("L2AGLb"),
                    By.xpath("//button[contains(.,'Accept all')]"),
                    By.xpath("//button[contains(.,'I agree')]"),
                    By.xpath("//button[contains(.,'Aceptar todo')]"),
                    By.xpath("//button[contains(.,'Acepto')]")
            );

            for (By b : buttons) {
                try {
                    WebElement btn = shortWait.until(ExpectedConditions.elementToBeClickable(b));
                    btn.click();
                    return;
                } catch (TimeoutException ignored) {
                }
            }

            try {
                WebElement iframe = driver.findElement(By.cssSelector("iframe[src*='consent']"));
                driver.switchTo().frame(iframe);

                for (By b : buttons) {
                    try {
                        WebElement btn = shortWait.until(ExpectedConditions.elementToBeClickable(b));
                        btn.click();
                        driver.switchTo().defaultContent();
                        return;
                    } catch (TimeoutException ignored) {
                    }
                }
                driver.switchTo().defaultContent();
            } catch (NoSuchElementException ignored) {
            }

        } catch (Exception ignored) {
        }
    }

    private void takeScreenshot(String fileName, By locator) throws IOException {
        try {
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            BufferedImage img = ImageIO.read(srcFile);

            WebElement highlightElement = null;
            try {
                highlightElement = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            } catch (StaleElementReferenceException e) {
                System.out.println("Elemento obsoleto antes de la captura, no se resaltará.");
            } catch (TimeoutException e) {
                System.out.println("No se encontró el elemento para resaltar en: " + fileName);
            }

            if (highlightElement != null && highlightElement.isDisplayed()) {
                // Obtener las coordenadas usando JavaScript para mayor precisión (viewport-relative)
                @SuppressWarnings("unchecked")
                Map<String, Object> rect = (Map<String, Object>) ((JavascriptExecutor) driver)
                        .executeScript("var el = arguments[0]; " +
                                "var rect = el.getBoundingClientRect(); " +
                                "return {x: rect.left, y: rect.top, width: rect.width, height: rect.height};",
                                highlightElement);

                int x = ((Number) rect.get("x")).intValue();
                int y = ((Number) rect.get("y")).intValue();
                int width = ((Number) rect.get("width")).intValue();
                int height = ((Number) rect.get("height")).intValue();

                Graphics2D g2d = img.createGraphics();
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRect(x, y, width, height);
                g2d.dispose();
            }

            File outputDir = new File("screenshots");
            if (!outputDir.exists()) outputDir.mkdirs();
            File outputFile = new File(outputDir, fileName);
            ImageIO.write(img, "png", outputFile);

        } catch (IOException e) {
            System.out.println("Error al guardar la captura de pantalla: " + fileName);
        }
    }

    private WebElement clickElementWithRetry(By locator) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                WebElement element = wait.until(
                        ExpectedConditions.refreshed(ExpectedConditions.elementToBeClickable(locator))
                );

                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);

                try {
                    element.click();
                } catch (ElementClickInterceptedException e) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                }

                return element;

            } catch (StaleElementReferenceException e) {
                System.out.println("Elemento obsoleto, reintentando... (" + (attempts + 1) + "/3)");
            }
            attempts++;
        }
        throw new RuntimeException("No se pudo hacer clic en el elemento después de 3 intentos.");
    }

    private void shortPause() throws InterruptedException {
        int randomSleep = ThreadLocalRandom.current().nextInt(150, 450);
        Thread.sleep(randomSleep);
    }

    private static class MenuItem {
        final String name;
        final String href;

        MenuItem(String name, String href) {
            this.name = name;
            this.href = href;
        }
    }
}