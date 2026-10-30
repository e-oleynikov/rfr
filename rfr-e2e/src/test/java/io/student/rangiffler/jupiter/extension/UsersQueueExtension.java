package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.jupiter.annotation.StaticUser;
import io.student.rangiffler.jupiter.fixture.StaticUserFixture;
import io.student.rangiffler.jupiter.fixture.TestUser;
import io.student.rangiffler.jupiter.fixture.UserFixture;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public final class UsersQueueExtension implements BeforeEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(UsersQueueExtension.class);

  @Override
  public void beforeEach(ExtensionContext context) {
    var parameters = context.getRequiredTestMethod().getParameters();
    for (int index = 0; index < parameters.length; index++) {
      if (!parameters[index].isAnnotationPresent(StaticUser.class)) {
        continue;
      }
      if (parameters[index].getType() != TestUser.class) {
        throw new ExtensionConfigurationException("@StaticUser is supported only for TestUser parameters");
      }
      UserFixture fixture = new StaticUserFixture();
      context.getStore(NAMESPACE).put(index, fixture);
      fixture.create();
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.isAnnotated(StaticUser.class)
        && parameterContext.getParameter().getType() == TestUser.class;
  }

  @Override
  public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return extensionContext.getStore(NAMESPACE)
        .get(parameterContext.getIndex(), UserFixture.class).user();
  }
}
