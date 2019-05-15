package org.dotwebstack.framework.backend.rdf4j.query;

import com.google.common.collect.ImmutableList;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnmodifiedType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.text.StringSubstitutor;
import org.dotwebstack.framework.backend.rdf4j.directives.Rdf4jDirectives;
import org.dotwebstack.framework.backend.rdf4j.shacl.NodeShapeRegistry;
import org.dotwebstack.framework.core.directives.ConstraintTraverser;
import org.dotwebstack.framework.core.directives.DirectiveUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;

@Slf4j
@RequiredArgsConstructor
public final class QueryFetcher implements DataFetcher<Object> {

  private static final ValueFactory VF = SimpleValueFactory.getInstance();

  private final RepositoryConnection repositoryConnection;

  private final NodeShapeRegistry nodeShapeRegistry;

  private final Map<String, String> prefixMap;

  private final JexlEngine jexlEngine;

  private final ConstraintTraverser constraintTraverser;

  @Override
  public Object get(@NonNull DataFetchingEnvironment environment) {
    GraphQLDirective sparqlDirective = environment.getFieldDefinition()
        .getDirective(Rdf4jDirectives.SPARQL_NAME);
    GraphQLType outputType = GraphQLTypeUtil.unwrapNonNull(environment.getFieldType());
    GraphQLUnmodifiedType rawType = GraphQLTypeUtil.unwrapAll(outputType);

    if (!(rawType instanceof GraphQLObjectType)) {
      throw new UnsupportedOperationException(
          "Field types other than object fields are not yet supported.");
    }

    constraintTraverser.traverse(environment);

    QueryEnvironment queryEnvironment = QueryEnvironment.builder()
        .objectType((GraphQLObjectType) rawType)
        .selectionSet(environment.getSelectionSet())
        .nodeShapeRegistry(nodeShapeRegistry)
        .prefixMap(prefixMap)
        .build();

    // Find shapes matching request
    List<IRI> subjects = fetchSubjects(queryEnvironment, sparqlDirective,
        environment.getArguments(), repositoryConnection);

    // Fetch graph for given subjects
    Model model = fetchGraph(queryEnvironment, subjects, repositoryConnection);

    if (GraphQLTypeUtil.isList(outputType)) {
      return subjects.stream()
          .map(subject -> new QuerySolution(model, subject))
          .collect(Collectors.toList());
    }

    return model.isEmpty() ? null : new QuerySolution(model, subjects.get(0));
  }

  private List<IRI> fetchSubjects(QueryEnvironment environment, GraphQLDirective sparqlDirective,
      Map<String, Object> arguments, RepositoryConnection con) {
    String subjectTemplate = DirectiveUtils
        .getArgument(Rdf4jDirectives.SPARQL_ARG_SUBJECT, sparqlDirective, String.class);

    if (subjectTemplate != null) {
      StringSubstitutor substitutor = new StringSubstitutor(arguments);
      IRI subject = VF.createIRI(substitutor.replace(subjectTemplate));

      return ImmutableList.of(subject);
    }

    String subjectQuery = SubjectQueryBuilder
        .create(environment, jexlEngine)
        .getQueryString(arguments, sparqlDirective);

    LOG.debug("Executing query for subjects:\n{}", subjectQuery);

    TupleQueryResult queryResult = con
        .prepareTupleQuery(subjectQuery)
        .evaluate();

    return QueryResults.asList(queryResult)
        .stream()
        .map(bindings -> (IRI) bindings.getValue("s"))
        .collect(Collectors.toList());
  }

  private Model fetchGraph(QueryEnvironment environment, List<IRI> subjects,
      RepositoryConnection con) {
    if (subjects.isEmpty()) {
      return new TreeModel();
    }

    String graphQuery = GraphQueryBuilder
        .create(environment, subjects)
        .getQueryString();

    LOG.debug("Executing query for graph:\n{}", graphQuery);

    GraphQueryResult queryResult = con
        .prepareGraphQuery(graphQuery)
        .evaluate();

    return QueryResults.asModel(queryResult);
  }

}