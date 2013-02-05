package com.sugarcrm.voodoo.automation;

import java.awt.Toolkit;
import java.io.File;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import org.openqa.selenium.Alert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;

import com.sugarcrm.voodoo.automation.control.VControl;
import com.sugarcrm.voodoo.automation.control.VHook;
import com.sugarcrm.voodoo.automation.control.VHook.Strategy;
import com.sugarcrm.voodoo.automation.control.VSelect;
import com.sugarcrm.voodoo.utilities.Utils;

public class VInterface implements IInterface {

	public final WebDriver wd;
	
	private final Voodoo voodoo;
	private final Properties props;
	private HashMap<Integer, String> windowHandles = new HashMap<Integer, String>();
	private int windowIndex = 0;

	/**
    * Instantiate VInterface
    *
	 * @param voodoo  {@link Voodoo} object
	 * @param props   {@link Properties} for this test run
	 * @param iType   {@link IInterface.Type} of web browser to run
	 * @throws Exception
	 */
	public VInterface(Voodoo voodoo, Properties props, IInterface.Type iType)
			throws Exception {
		this.voodoo = voodoo;
		this.props = props;
		this.wd = this.getWebDriver(iType);
		this.start();
	}
	
	@Override
	public void pause(long ms) throws Exception {
		voodoo.log.info("Pausing for " + ms + "ms via thread sleep.");
		Thread.sleep(ms);
	}
	
	@Override
	public void interact(String message) {
		voodoo.log.info("Interaction via popup dialog with message: " + message);
		JOptionPane.showInputDialog(message);
	}
	
	@Override
	public void start() throws Exception {
		voodoo.log.info("Starting browser.");
	}

	@Override
	public void stop() throws Exception {
		voodoo.log.info("Stopping automation.");
		this.wd.quit();
	}

	public void closeWindow() throws Exception {
		voodoo.log.info("Closing window.");
		this.wd.close();
	}

	@Override
	public void go(String url) throws Exception {
		voodoo.log.info("Going to URL and switching to window: " + url);
		this.wd.get(url);
		this.wd.switchTo().window(this.wd.getWindowHandle());
	}

	@Override
	public void acceptDialog() throws Exception {
		voodoo.log.info("Accepting dialog.");
		Alert alert = this.wd.switchTo().alert();
		alert.accept();
	}

	@Override
	public void focusByIndex(int index) throws Exception {
		voodoo.log.info("Focusing window by index: " + index);
		Set<String> Handles = this.wd.getWindowHandles();
		while (Handles.iterator().hasNext()) {
			String windowHandle = Handles.iterator().next();
			if (!windowHandles.containsValue(windowHandle)) {
				windowHandles.put(windowIndex, windowHandle);
				windowIndex++;
			}
			Handles.remove(windowHandle);
		}
		this.wd.switchTo().window(windowHandles.get(index));
	}

	@Override
	public void focusByTitle(String title) throws Exception {
		voodoo.log.info("Focusing window by title: " + title);
		Set<String> handles = this.wd.getWindowHandles();
		while (handles.iterator().hasNext()) {
			String windowHandle = handles.iterator().next();
			WebDriver window = this.wd.switchTo().window(windowHandle);
			if (window.getTitle().equals(title)) {
				break;
			}
			handles.remove(windowHandle);
		}
	}

	@Override
	public void focusByUrl(String url) throws Exception {
		voodoo.log.info("Focusing window by url: " + url);
		Set<String> handles = this.wd.getWindowHandles();
		while (handles.iterator().hasNext()) {
			String windowHandle = handles.iterator().next();
			WebDriver window = this.wd.switchTo().window(windowHandle);
			if (window.getCurrentUrl().equals(url)) {
				break;
			}
			handles.remove(windowHandle);
		}
	}

	@Override
	public void maximize() {
		voodoo.log.info("Maximizing window");
		java.awt.Dimension screenSize = Toolkit.getDefaultToolkit()
				.getScreenSize();
		this.wd.manage().window().setSize(new Dimension(screenSize.width, screenSize.height));
	}

	@Override
	public VControl getControl(VHook hook) throws Exception {
		return new VControl(this.voodoo, this, hook);
	}

	@Override
	public VControl getControl(Strategy strategy, String hook) throws Exception {
		return this.getControl(new VHook(strategy, hook));
	}

	@Override
	public VSelect getSelect(VHook hook) throws Exception {
		return new VSelect(this.voodoo, this, hook);
	}

	@Override
	public VSelect getSelect(Strategy strategy, String hook) throws Exception {
		return this.getSelect(new VHook(strategy, hook));
	}
	
	private WebDriver getWebDriver(IInterface.Type iType) throws Exception {
		WebDriver wd = null;
		switch (iType) {
		case FIREFOX:
			String profileName = Utils.getCascadingPropertyValue(this.props,
					"default", "browser.firefox_profile");
			String ffBinaryPath = Utils.getCascadingPropertyValue(this.props,
					"//home//conrad//Applications//firefox-10//firefox",
					"browser.firefox_binary");
			FirefoxProfile ffProfile = (new ProfilesIni())
					.getProfile(profileName);
			FirefoxBinary ffBinary = new FirefoxBinary(new File(ffBinaryPath));
			// if (System.getProperty("headless") != null) {
			// FirefoxBinary ffBinary = new FirefoxBinary();//new
			// File("//home//conrad//Applications//firefox-10//firefox"));
			// ffBinary.setEnvironmentProperty("DISPLAY", ":1");
			// webDriver = new FirefoxDriver(ffBinary, ffProfile);
			// }
			voodoo.log.info("Instantiating Firefox with profile name: "
					+ profileName + " and binary path: " + ffBinaryPath);
			wd = new FirefoxDriver(ffBinary, ffProfile);
			break;
		case CHROME:
			String workingDir = System.getProperty("user.dir");
			ChromeOptions chromeOptions = new ChromeOptions();
			String chromeDriverLogPath = Utils.getCascadingPropertyValue(props,
					workingDir + "/log/chromedriver.log",
					"browser.chrome_driver_log_path");
			chromeOptions.addArguments("--log-path=" + chromeDriverLogPath);
			String chromeDriverPath = Utils.getCascadingPropertyValue(props,
					workingDir + "/etc/chromedriver-mac",
					"browser.chrome_driver_path");
			// chromeOptions.setBinary(new File(chromeDriverPath));
			System.setProperty("webdriver.chrome.driver", chromeDriverPath);
			voodoo.log.info("Instantiating Chrome with:\n    log path:"
					+ chromeDriverLogPath + "\n    driver path: "
					+ chromeDriverPath);
			wd = new ChromeDriver(chromeOptions);
			break;
		case IE:
			throw new Exception("Selenium: ie browser not yet supported.");
		case SAFARI:
			throw new Exception("Selenium: safari browser not yet supported.");
		default:
			throw new Exception("Selenium: browser type not recognized.");
		}
		long implicitWait = Long.parseLong(props.getProperty("perf.implicit_wait"));
		if (System.getProperty("headless") == null) {
			java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			wd.manage().window().setSize(new Dimension(screenSize.width, screenSize.height));
		}
		wd.manage().timeouts().implicitlyWait(implicitWait, TimeUnit.SECONDS);
		return wd;
	}

  //	/**
  //	 * @param selectElement
  //	 * @param actionElement
  //	 */
  //	public static void allOptionsAction(Select selectElement, WebElement actionElement) {
  //		List<WebElement> options = selectElement.getOptions();
  //		for (WebElement option : options) {
  //			selectElement.selectByVisibleText(option.getText());
  //			actionElement.click();
  //		}
  //	}
  //	
  //	
  //	/**
  //	 * @param selectElement
  //	 * @param actionOptionValues
  //	 * @param actionElement
  //	 * @throws Exception
  //	 */
  //	public static void optionAction(Select selectElement, Set<String> actionOptionValues, WebElement actionElement) throws Exception {
  //		List<WebElement> allOptions = selectElement.getOptions();
  //		HashSet<String> optionValues = new HashSet<String>();
  //		for(WebElement option : allOptions) {
  //			optionValues.add(option.getText());
  ////			System.out.println("Adding to options set:" + option.getText());
  //		}
  //		if(optionValues.containsAll(actionOptionValues)) {
  //			for(String option : actionOptionValues) {
  //				selectElement.selectByVisibleText(option);
  //				actionElement.click();
  //			}
  //		} else throw new Exception("Specified select option unavailable...");
  //	}
  //	
  //	
  //
  //	/**
  //	 * @param element
  //	 * @return
  //	 */
  //	public static String webElementToString(WebElement element) {
  //		List<WebElement> childElements = element.findElements(By.xpath("*"));
  //		String s = element.getTagName() + ":" + element.getText() + " ";
  //		for(WebElement we : childElements) {
  //			s += we.getTagName() + ":" + we.getText() + " ";
  //		}
  //		return s;
  //	}
  //	
  //	
  //	/**
  //	 * @param nativeOptions
  //	 * @param queryOptionNames
  //	 * @return
  //	 */
  //	public static boolean optionValuesEqual(List<WebElement> nativeOptions, Set<String> queryOptionNames) {
  //		Set<String> nativeOptionNames = new HashSet<String>();
  //		for (WebElement option : nativeOptions) {
  //			nativeOptionNames.add(option.getText());
  //		}
  //		if (nativeOptionNames.containsAll(queryOptionNames) && queryOptionNames.containsAll(nativeOptionNames)) return true;
  //		else return false;
  //	}
}
