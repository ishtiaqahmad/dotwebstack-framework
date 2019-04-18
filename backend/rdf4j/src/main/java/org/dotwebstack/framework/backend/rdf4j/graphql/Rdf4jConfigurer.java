package org.dotwebstack.framework.backend.rdf4j.graphql;

import graphql.Scalars;
import graphql.introspection.Introspection;
import graphql.language.DirectiveDefinition;
import graphql.language.DirectiveLocation;
import graphql.language.InputValueDefinition;
import graphql.language.NonNullType;
import graphql.language.TypeName;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dotwebstack.framework.backend.rdf4j.graphql.directives.Rdf4jDirectives;
import org.dotwebstack.framework.backend.rdf4j.graphql.directives.SparqlDirectiveWiring;
import org.dotwebstack.framework.backend.rdf4j.graphql.scalars.Rdf4jScalars;
import org.dotwebstack.framework.backend.rdf4j.shacl.NodeShapeRegistry;
import org.dotwebstack.framework.core.graphql.GraphqlConfigurer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Rdf4jConfigurer implements GraphqlConfigurer {

  private final SparqlDirectiveWiring sparqlDirectiveWiring;

  private final NodeShapeRegistry nodeShapeRegistry;

  @Override
  public void configureTypeDefinitionRegistry(@NonNull TypeDefinitionRegistry registry) {
    TypeName optionalString = TypeName.newTypeName(Scalars.GraphQLString.getName()).build();
    NonNullType requiredString = NonNullType.newNonNullType(optionalString).build();

    registry.add(DirectiveDefinition.newDirectiveDefinition()
        .name(Rdf4jDirectives.SPARQL_NAME)
        .inputValueDefinition(InputValueDefinition.newInputValueDefinition()
            .name(Rdf4jDirectives.SPARQL_ARG_REPOSITORY)
            .type(requiredString)
            .build())
        .inputValueDefinition(InputValueDefinition.newInputValueDefinition()
            .name(Rdf4jDirectives.SPARQL_ARG_SUBJECT)
            .type(optionalString)
            .build())
        .inputValueDefinition(InputValueDefinition.newInputValueDefinition()
            .name("orderBy")
            .type(optionalString)
            .build())
        .directiveLocation(DirectiveLocation.newDirectiveLocation()
            .name(Introspection.DirectiveLocation.FIELD_DEFINITION.name())
            .build())
        .directiveLocation(DirectiveLocation.newDirectiveLocation()
            .name(Introspection.DirectiveLocation.OBJECT.name())
            .build())
        .build());
  }

  @Override
  public void configureRuntimeWiring(@NonNull RuntimeWiring.Builder builder) {
    builder
        .codeRegistry(registerValueFetchers())
        .scalar(Rdf4jScalars.IRI)
        .directive(Rdf4jDirectives.SPARQL_NAME, sparqlDirectiveWiring);
  }

  private GraphQLCodeRegistry registerValueFetchers() {
    GraphQLCodeRegistry.Builder builder = GraphQLCodeRegistry.newCodeRegistry();

    nodeShapeRegistry.all()
        .forEach(nodeShape -> {
          String typeName = nodeShape.getIdentifier().getLocalName();

          nodeShape.getPropertyShapes()
              .values()
              .forEach(propertyShape -> builder
                  .dataFetcher(FieldCoordinates.coordinates(typeName, propertyShape.getName()),
                      new ValueFetcher(propertyShape)));
        });

    return builder.build();
  }

}
