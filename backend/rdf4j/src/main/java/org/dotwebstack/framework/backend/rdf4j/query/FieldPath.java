package org.dotwebstack.framework.backend.rdf4j.query;

import static org.dotwebstack.framework.core.helpers.ExceptionHelper.illegalStateException;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLTypeUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import org.dotwebstack.framework.backend.rdf4j.directives.Rdf4jDirectives;

@Builder
@Getter
public class FieldPath {

  @Builder.Default
  private List<GraphQLFieldDefinition> fieldDefinitions = new ArrayList<>();

  private boolean required;

  public boolean isSingleton() {
    return fieldDefinitions.size() == 1;
  }

  public Optional<FieldPath> rest() {
    if (this.fieldDefinitions.size() > 1) {
      List<GraphQLFieldDefinition> rest = this.fieldDefinitions.subList(1, this.fieldDefinitions.size());
      return Optional.of(FieldPath.builder()
          .fieldDefinitions(rest)
          .build());
    }
    return Optional.empty();
  }

  public boolean isRequired() {
    return required || fieldDefinitions.stream()
        .allMatch(fieldDefinition -> GraphQLTypeUtil.isNonNull(fieldDefinition.getType()));
  }

  public boolean isResource() {
    if (isSingleton()) {
      return Objects.nonNull(fieldDefinitions.get(0)
          .getDirective(Rdf4jDirectives.RESOURCE_NAME));
    }
    return false;
  }

  public GraphQLFieldDefinition first() {
    if (!fieldDefinitions.isEmpty()) {
      return fieldDefinitions.get(0);
    }
    throw illegalStateException("No current fieldDefinition!");
  }

  public Optional<GraphQLFieldDefinition> last() {
    if (!fieldDefinitions.isEmpty()) {
      return Optional.of(fieldDefinitions.get(fieldDefinitions.size() - 1));
    }
    return Optional.empty();
  }
}
