package org.dotwebstack.framework.core.directives;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLDirectiveContainer;
import java.util.Objects;
import lombok.NonNull;
import org.dotwebstack.framework.core.InvalidConfigurationException;

public final class DirectiveUtils {

  private DirectiveUtils() {}

  public static <T> T getArgument(@NonNull GraphQLDirectiveContainer directiveContainer, @NonNull String directiveName,
      @NonNull String argumentName, @NonNull Class<T> clazz) {
    GraphQLDirective directive = directiveContainer.getDirective(directiveName);

    if (Objects.isNull(directive)) {
      return null;
    }
    return getArgument(directive, argumentName, clazz);
  }

  public static <T> T getArgument(@NonNull GraphQLDirective directive, @NonNull String argName,
      @NonNull Class<T> clazz) {
    final GraphQLArgument argument = directive.getArgument(argName);

    if (argument == null) {
      return null;
    }

    final Object argValue = argument.getValue();

    if (argValue == null) {
      return null;
    } else if (!clazz.isInstance(argValue)) {
      throw new InvalidConfigurationException("Argument type mismatch for '{}': expected[{}], but was [{}].", argName,
          clazz, argValue.getClass());
    } else {
      return clazz.cast(argValue);
    }
  }
}
