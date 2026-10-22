package io.student.rangiffler.extension;

import io.student.rangiffler.data.TestUserCleaner;
import io.student.rangiffler.model.TestUser;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public class TestUserExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(TestUserExtension.class);

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    TestUserCleaner cleaner = new TestUserCleaner();
    TestUser user = TestUser.unique();
    // Check DB access and username uniqueness before registration can create any data.
    cleaner.verifyUserAbsent(user.username());
    context.getStore(NAMESPACE).put(TestUserCleaner.class, cleaner);
    context.getStore(NAMESPACE).put(TestUser.class, user);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    TestUser user = context.getStore(NAMESPACE).remove(TestUser.class, TestUser.class);
    TestUserCleaner cleaner = context.getStore(NAMESPACE).remove(TestUserCleaner.class, TestUserCleaner.class);
    if (user != null && cleaner != null) {
      cleaner.delete(user.username());
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType() == TestUser.class;
  }

  @Override
  public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return extensionContext.getStore(NAMESPACE).get(TestUser.class, TestUser.class);
  }
}
