package stockScraperApp;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import entity.Stock;
import entity.User;

public class ScrapeStockSite {

	public static void main(String[] args) throws MalformedURLException, InterruptedException {

		SessionFactory factory = new Configuration().configure("hibernate.cfg.xml").addAnnotatedClass(Stock.class)
				.addAnnotatedClass(User.class).buildSessionFactory();

		// get the instructor from db
		Session session = factory.getCurrentSession();
		session.beginTransaction();

		int theId = 1;
		User currentUser = null;
		try {
			currentUser = session.get(User.class, theId);
		}finally {
			session.getTransaction().commit();
			session.close();
		}

		ChromeOptions chromeOptions = new ChromeOptions();
		URL url = new URL("http://192.168.99.101:4444/wd/hub");
		RemoteWebDriver driver = new RemoteWebDriver(url, chromeOptions);

		driver.get("https://finance.yahoo.com/");

		System.out.println("Website successfully accessed...\n");

		WebDriverWait wait = new WebDriverWait(driver, 60);
		WebElement signIn = wait.until(ExpectedConditions.elementToBeClickable(By.id("uh-signedin")));
		signIn.click();

		System.out.println("Entering username...\n");

		WebElement usernameTextbox = wait.until(ExpectedConditions.elementToBeClickable(By.name("username")));
		usernameTextbox.clear();
		usernameTextbox.sendKeys(currentUser.getUserName());

		WebElement logIn = wait.until(ExpectedConditions.elementToBeClickable(By.name("signin")));
		driver.executeScript("arguments[0].click()", logIn);

		System.out.println("Entering password...\n");

		WebElement password = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login-passwd")));

		password.sendKeys(currentUser.getPassword());

		WebElement passwdSignIn = wait.until(ExpectedConditions.elementToBeClickable(By.id("login-signin")));

		driver.executeScript("arguments[0].click()", passwdSignIn);

		System.out.println("Login successful! Proceeding to portfolio...\n");

		WebElement portfolios = wait.until(ExpectedConditions.elementToBeClickable(By.partialLinkText("Portfolio")));
		driver.executeScript("arguments[0].click()", portfolios);

		WebElement myPortfolio = driver.findElementByPartialLinkText(currentUser.getPortfolioName());
		driver.executeScript("arguments[0].click()", myPortfolio);

		wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("td[aria-label='Symbol']")));
		wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("td[aria-label='Last Price']")));
		wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("td[aria-label='Chg %']")));

		System.out.println("Portfolio successfully accessed! Beginning data scrape...\n");

		ArrayList<String> symbolsText = new ArrayList<String>();
		ArrayList<String> pricesText = new ArrayList<String>();
		ArrayList<String> changesText = new ArrayList<String>();

		List<WebElement> symbols = driver.findElementsByCssSelector("td[aria-label='Symbol']");
		List<WebElement> prices = driver.findElementsByCssSelector("td[aria-label='Last Price']");
		List<WebElement> changes = driver.findElementsByCssSelector("td[aria-label='Chg %']");

		for (int i = 0; i < symbols.size(); i++) {
			symbolsText.add(symbols.get(i).getText());
			pricesText.add(prices.get(i).getText());
			changesText.add(changes.get(i).getText());
		}

		Session scrapSession = factory.getCurrentSession();
		scrapSession.beginTransaction();

		LocalDateTime scrapeTime = LocalDateTime.now();

		try {
			for (int i = 0; i < symbols.size(); i++) {

				Stock stock = new Stock(symbolsText.get(i), pricesText.get(i), changesText.get(i), scrapeTime);
				stock.setUser(currentUser);
				scrapSession.save(stock);
				System.out.println(stock.toString());

			}

			scrapSession.getTransaction().commit();

		} catch (Exception e) {
			System.out.println(e);
		} finally {
			driver.manage().deleteAllCookies();
			scrapSession.close();
			factory.close();
		}
	}
}
