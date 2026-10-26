package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.student.rangiffler.config.TestConfig;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public class BrowserExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(BrowserExtension.class);

  @Override
  public void beforeEach(ExtensionContext context) {
    Playwright playwright = Playwright.create();
    context.getStore(NAMESPACE).put(Playwright.class, playwright);
    Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
        .setHeadless(!TestConfig.headed()));
    BrowserContext browserContext = browser.newContext(new Browser.NewContextOptions()
        .setBaseURL(TestConfig.frontUrl()));
    context.getStore(NAMESPACE).put(BrowserContext.class, browserContext);
    context.getStore(NAMESPACE).put(Page.class, browserContext.newPage());
  }

  @Override
  public void afterEach(ExtensionContext context) {
    context.getStore(NAMESPACE).remove(Page.class, Page.class);
    BrowserContext browserContext = context.getStore(NAMESPACE).remove(BrowserContext.class, BrowserContext.class);
    Playwright playwright = context.getStore(NAMESPACE).remove(Playwright.class, Playwright.class);
    try {
      if (browserContext != null) {
        browserContext.close();
      }
    } finally {
      if (playwright != null) {
        playwright.close();
      }
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType() == Page.class;
  }

  @Override
  public Page resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return extensionContext.getStore(NAMESPACE).get(Page.class, Page.class);
  }
}
