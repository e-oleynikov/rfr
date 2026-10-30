package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.data.TestUserRepository;
import io.student.rangiffler.jupiter.fixture.UserFixture;
import io.student.rangiffler.jupiter.fixture.TestUser;
import io.student.rangiffler.utils.RandomDataUtils;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;

public class TestUserExtension implements BeforeEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(TestUserExtension.class);

  @Override
  public void beforeEach(ExtensionContext context) {
    Parameter[] parameters = context.getRequiredTestMethod().getParameters();
    for (int index = 0; index < parameters.length; index++) {
      if (!parameters[index].isAnnotationPresent(User.class)) {
        continue;
      }
      if (parameters[index].getType() != TestUser.class) {
        throw new ExtensionConfigurationException("@User is supported only for TestUser parameters");
      }
      UserFixture fixture = new UserFixture(new TestUserRepository(), RandomDataUtils.randomUser());
      // Keep the fixture in the method store: JUnit 6 closes it even if create() fails.
      context.getStore(NAMESPACE).put(index, fixture);
      fixture.create();
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.isAnnotated(User.class)
        && parameterContext.getParameter().getType() == TestUser.class;
  }

  @Override
  public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return extensionContext.getStore(NAMESPACE).get(parameterContext.getIndex(), UserFixture.class).user();
  }
}
