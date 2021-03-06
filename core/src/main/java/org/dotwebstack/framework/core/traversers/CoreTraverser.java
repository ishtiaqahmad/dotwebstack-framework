package org.dotwebstack.framework.core.traversers;

import static java.util.Collections.emptyList;
import static org.dotwebstack.framework.core.helpers.MapHelper.getNestedMap;

import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.language.TypeName;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.dotwebstack.framework.core.helpers.TypeHelper;
import org.springframework.stereotype.Component;

@Component
public class CoreTraverser {

  private static final List<String> SUPPORTED_TYPE_NAMES = Arrays.asList("IRI", "Model", "SparqlQueryResult");

  private final TypeDefinitionRegistry typeDefinitionRegistry;

  public CoreTraverser(TypeDefinitionRegistry typeDefinitionRegistry) {
    this.typeDefinitionRegistry = typeDefinitionRegistry;
  }

  public List<DirectiveContainerObject> getTuples(@NonNull GraphQLFieldDefinition fieldDefinition,
      @NonNull Map<String, Object> arguments, @NonNull TraverserFilter filter) {

    return fieldDefinition.getArguments()
        .stream()
        .flatMap(argument -> getInputObjectFieldsFromArgument(fieldDefinition, argument, arguments).stream())
        .filter(filter::apply)
        .collect(Collectors.toList());
  }

  public List<TypeName> getRootResultTypeNames(@NonNull TypeDefinition<?> baseType) {
    return typeDefinitionRegistry.types()
        .values()
        .stream()
        .flatMap(typeDefinition -> {
          if (typeDefinition instanceof ObjectTypeDefinition) {
            return traverseObjectType(baseType, (ObjectTypeDefinition) typeDefinition).stream();
          } else if (typeDefinition instanceof InputObjectTypeDefinition) {
            return traverseInputObjectType(baseType, typeDefinition).stream();
          }
          return Stream.empty();
        })
        .collect(Collectors.toList());
  }

  private List<DirectiveContainerObject> getInputObjectFieldsFromArgument(GraphQLFieldDefinition fieldDefinition,
      GraphQLArgument container, Map<String, Object> requestArguments) {

    String inputTypeName = GraphQLTypeUtil.unwrapAll(fieldDefinition.getType())
        .getName();

    if (SUPPORTED_TYPE_NAMES.contains(inputTypeName)) {
      return emptyList();
    }

    GraphQLObjectType objectType = (GraphQLObjectType) GraphQLTypeUtil.unwrapAll(fieldDefinition.getType());
    List<DirectiveContainerObject> result = new ArrayList<>();
    if (container.getType() instanceof GraphQLInputObjectType) {
      Map<String, Object> nestedArguments = getNestedMap(requestArguments, container.getName());
      result.addAll(getInputObjectFieldsFromObjectType((GraphQLInputObjectType) container.getType(), nestedArguments,
          fieldDefinition));

      result
          .add(getDirectiveContainerObject(container, objectType, nestedArguments, fieldDefinition, requestArguments));

    } else if ((GraphQLTypeUtil.unwrapAll(container.getType()) instanceof GraphQLScalarType)) {
      Object nestedArguments = requestArguments.getOrDefault(container.getName(), container.getDefaultValue());

      result
          .add(getDirectiveContainerObject(container, objectType, nestedArguments, fieldDefinition, requestArguments));
    }

    return result;
  }

  private DirectiveContainerObject getDirectiveContainerObject(GraphQLArgument container, GraphQLObjectType objectType,
      Object nestedArguments, GraphQLFieldDefinition fieldDefinition, Map<String, Object> requestArguments) {
    return DirectiveContainerObject.builder()
        .container(container)
        .objectType(objectType)
        .value(nestedArguments)
        .fieldDefinition(fieldDefinition)
        .requestArguments(requestArguments)
        .build();
  }

  /*
   * return a list containing the input object types that can be reached top down from a given input
   * object type
   */
  private List<DirectiveContainerObject> getInputObjectFieldsFromObjectType(GraphQLInputObjectType inputObjectType,
      Map<String, Object> arguments, GraphQLFieldDefinition fieldDefinition) {
    return inputObjectType.getFields()
        .stream()
        .flatMap(field -> {
          if (field.getType() instanceof GraphQLInputObjectType) {
            return getInputObjectFieldsFromObjectType((GraphQLInputObjectType) field.getType(),
                getNestedMap(arguments, field.getName()), fieldDefinition).stream();
          } else if (GraphQLTypeUtil.unwrapAll(field.getType()) instanceof GraphQLScalarType) {
            return Stream.of(DirectiveContainerObject.builder()
                .container(field)
                .value(arguments.getOrDefault(field.getName(), null))
                .fieldDefinition(fieldDefinition)
                .build());
          }
          return Stream.empty();
        })
        .collect(Collectors.toList());
  }

  /*
   * return the list containing the input object types from the given parent type that match the
   * compare type
   */
  private List<TypeName> traverseObjectType(TypeDefinition<?> compareType, ObjectTypeDefinition parentType) {
    return parentType.getFieldDefinitions()
        .stream()
        .filter(inputField -> inputField.getInputValueDefinitions()
            .stream()
            .anyMatch(inputValueDefinition -> typeDefinitionRegistry
                .getType(TypeHelper.getBaseType(inputValueDefinition.getType()))
                .map(definition -> definition.equals(compareType))
                .orElse(false)))
        .map(inputField -> ((TypeName) TypeHelper.getBaseType(inputField.getType())))
        .collect(Collectors.toList());
  }

  /*
   * return the list containing the input object types from the given parent type (recursive) that
   * match the compare type
   */
  private List<TypeName> traverseInputObjectType(TypeDefinition<?> compareType, TypeDefinition<?> parentType) {
    Optional<InputValueDefinition> inputValueDefinition =
        ((InputObjectTypeDefinition) parentType).getInputValueDefinitions()
            .stream()
            .filter(inputValue -> typeDefinitionRegistry.getType(TypeHelper.getBaseType(inputValue.getType()))
                .map(definition -> definition.equals(compareType))
                .orElse(false))
            .findAny();

    if (inputValueDefinition.isPresent()) {
      return getRootResultTypeNames(parentType);
    }
    return emptyList();
  }

}
