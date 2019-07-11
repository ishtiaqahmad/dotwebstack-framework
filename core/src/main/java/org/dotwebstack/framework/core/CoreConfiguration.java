package org.dotwebstack.framework.core;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import lombok.NonNull;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
@EnableConfigurationProperties(CoreProperties.class)
public class CoreConfiguration {

  private static final String FIXED_SCHEMA_NAME = "schema.graphqls";

  @Bean
  public GraphQLSchema graphqlSchema(@NonNull TypeDefinitionRegistry typeDefinitionRegistry,
      @NonNull Collection<GraphqlConfigurer> graphqlConfigurers) {

    RuntimeWiring.Builder runtimeWiringBuilder = RuntimeWiring.newRuntimeWiring();
    graphqlConfigurers.forEach(graphqlConfigurer -> graphqlConfigurer.configureRuntimeWiring(runtimeWiringBuilder));

    graphqlConfigurers
        .forEach(graphqlConfigurer -> graphqlConfigurer.configureTypeDefinitionRegistry(typeDefinitionRegistry));

    return new SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiringBuilder.build());
  }

  @Bean
  public TypeDefinitionRegistry typeDefinitionRegistry(@NonNull ResourceLoader resourceLoader,
      @NonNull CoreProperties coreProperties) throws IOException {
    Reader reader = new InputStreamReader(resourceLoader.getResource(coreProperties.getResourcePath()
        .resolve(FIXED_SCHEMA_NAME)
        .toString())
        .getInputStream());

    return new SchemaParser().parse(reader);
  }

  @Bean
  public GraphQL graphql(@NonNull GraphQLSchema graphqlSchema) {
    return GraphQL.newGraphQL(graphqlSchema)
        .build();
  }

  @Bean
  public JexlEngine jexlBuilder() {
    return new JexlBuilder().silent(false)
        .strict(true)
        .create();
  }
}
