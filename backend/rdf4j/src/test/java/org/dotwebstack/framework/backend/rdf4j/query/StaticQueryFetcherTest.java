package org.dotwebstack.framework.backend.rdf4j.query;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLOutputType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.dotwebstack.framework.backend.rdf4j.RepositoryAdapter;
import org.dotwebstack.framework.backend.rdf4j.converters.BooleanConverter;
import org.dotwebstack.framework.backend.rdf4j.converters.ByteConverter;
import org.dotwebstack.framework.backend.rdf4j.converters.DateConverter;
import org.dotwebstack.framework.backend.rdf4j.converters.DateTimeConverter;
import org.dotwebstack.framework.backend.rdf4j.converters.DecimalConverter;
import org.dotwebstack.framework.backend.rdf4j.converters.DoubleConverter;
import org.dotwebstack.framework.backend.rdf4j.converters.FloatConverter;
import org.dotwebstack.framework.backend.rdf4j.converters.IntConverter;
import org.dotwebstack.framework.backend.rdf4j.converters.IntegerConverter;
import org.dotwebstack.framework.backend.rdf4j.converters.IriConverter;
import org.dotwebstack.framework.backend.rdf4j.converters.LocalDateConverter;
import org.dotwebstack.framework.backend.rdf4j.converters.LongConverter;
import org.dotwebstack.framework.backend.rdf4j.converters.Rdf4jConverterRouter;
import org.dotwebstack.framework.backend.rdf4j.converters.ShortConverter;
import org.dotwebstack.framework.backend.rdf4j.converters.StringConverter;
import org.dotwebstack.framework.backend.rdf4j.directives.Rdf4jDirectives;
import org.dotwebstack.framework.backend.rdf4j.scalars.Rdf4jScalars;
import org.dotwebstack.framework.core.InvalidConfigurationException;
import org.dotwebstack.framework.core.NotImplementedException;
import org.dotwebstack.framework.core.converters.CoreConverter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

class StaticQueryFetcherTest {

  private static final String SELECT_QUERY = "SELECT DISTINCT ?breweries_with_query_ref_as_iri WHERE {\n"
      + "   ?subject <https://github.com/dotwebstack/beer/def#brewery> ?breweries_with_query_ref_as_iri\n" + "}";

  @Mock
  private RepositoryAdapter mockRepositoryAdapterMock;

  @Mock
  private DataFetchingEnvironment dataFetchingEnvironmentMock;

  @Mock
  private GraphQLFieldDefinition graphQlFieldDefinitionMock;

  @Mock
  private TupleQuery tupleQueryMock;

  @Mock
  private TupleQueryResult tupleQueryResultMock;

  @Mock
  private GraphQuery graphQueryMock;

  @Mock
  private GraphQueryResult graphQueryResultMock;

  private StaticQueryFetcher staticQueryFetcherUnderTest;

  @BeforeEach
  void setUp() {
    initMocks(this);

    List<CoreConverter<Value, ?>> converters =
        List.of(new ByteConverter(), new StringConverter(), new BooleanConverter(), new ByteConverter(),
            new DecimalConverter(), new DateConverter(), new DoubleConverter(), new FloatConverter(),
            new IntConverter(), new IntegerConverter(), new IriConverter(), new LongConverter(), new ShortConverter());

    Rdf4jConverterRouter converterRouter = new Rdf4jConverterRouter(converters);

    staticQueryFetcherUnderTest =
        new StaticQueryFetcher(mockRepositoryAdapterMock, Collections.emptyList(), converterRouter, SELECT_QUERY);
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getForIriTest(GraphQLOutputType graphqlOutputType) {
    testGetSuccessful(graphqlOutputType, "IRI", "\"https://github.com/dotwebstack/beer/id/beer/6\"");
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getForIdTest(GraphQLOutputType graphqlOutputType) {
    testGetSuccessful(graphqlOutputType, "ID", "\"https://github.com/dotwebstack/beer/id/beer/6\"");
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getForStringTest(GraphQLOutputType graphqlOutputType) {
    testGetSuccessful(graphqlOutputType, "String", "\"https://github.com/dotwebstack/beer/id/beer/6\"");
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getForBooleanTest(GraphQLOutputType graphqlOutputType) {
    testGetSuccessful(graphqlOutputType, "Boolean", true);
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getForByteTest(GraphQLOutputType graphqlOutputType) {
    testGetSuccessful(graphqlOutputType, "Byte", Byte.parseByte("2"));
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getForIntTest(GraphQLOutputType graphqlOutputType) {
    testGetSuccessful(graphqlOutputType, "Int", 1);
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getForBigIntegerTest(GraphQLOutputType graphqlOutputType) {
    testGetSuccessful(graphqlOutputType, "BigInteger", BigInteger.TEN);
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getForShortTest(GraphQLOutputType graphqlOutputType) {
    testGetSuccessful(graphqlOutputType, "Short", (short) 1);
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getForFloatTest(GraphQLOutputType graphqlOutputType) {
    testGetSuccessful(graphqlOutputType, "Float", 1.0f);
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getForDoubleTest(GraphQLOutputType graphqlOutputType) {
    testGetSuccessful(graphqlOutputType, "Double", 1.0);
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getForDateTest(GraphQLOutputType graphqlOutputType) {
    testGetSuccessful(graphqlOutputType, "Date", new Date(System.currentTimeMillis()));
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getForBigDecimalTest(GraphQLOutputType graphqlOutputType) {
    testGetSuccessful(graphqlOutputType, "BigDecimal", BigDecimal.ONE);
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getForLongTest(GraphQLOutputType graphqlOutputType) {
    testGetSuccessful(graphqlOutputType, "Long", Long.MAX_VALUE);
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void getUnsupportedTest(GraphQLOutputType graphqlOutputType) {
    testGetUnsuccessful(graphqlOutputType, "UnsupportedType", "value");
  }

  @Test
  void getExceptionForDateTimeConverterTest() {
    // Arrange
    DateTimeConverter dateTimeConverter = new DateTimeConverter();

    // Act / Assert
    assertThrows(NotImplementedException.class, () -> dateTimeConverter.convertToValue("someData"));
  }

  @Test
  void getExceptionForLocateDateConverterTest() {
    // Arrange
    LocalDateConverter localDateConverter = new LocalDateConverter();

    // Act / Assert
    assertThrows(NotImplementedException.class, () -> localDateConverter.convertToValue("someData"));
  }

  private void testGetSuccessful(GraphQLOutputType graphqlOutputType, String type, Object value) {
    // Arrange
    arrange(graphqlOutputType, type, value);

    // Act
    assertDoesNotThrow(() -> staticQueryFetcherUnderTest.get(dataFetchingEnvironmentMock));
  }

  private void testGetUnsuccessful(GraphQLOutputType graphqlOutputType, String type, Object value) {
    // Arrange
    arrange(graphqlOutputType, type, value);

    // Act
    assertThrows(InvalidConfigurationException.class,
        () -> staticQueryFetcherUnderTest.get(dataFetchingEnvironmentMock));
  }

  private void arrange(GraphQLOutputType graphqlOutputType, String type, Object value) {
    List<GraphQLArgument> arguments = new ArrayList<>();
    GraphQLInputObjectType iriType = GraphQLInputObjectType.newInputObject()
        .name(type)
        .build();

    GraphQLArgument graphQlArgument = GraphQLArgument.newArgument()
        .name("subject")
        .type(iriType)
        .build();

    arguments.add(graphQlArgument);

    GraphQLInputObjectType stringType = GraphQLInputObjectType.newInputObject()
        .name("String")
        .build();

    GraphQLDirective directive = GraphQLDirective.newDirective()
        .name("sparql")
        .argument(GraphQLArgument.newArgument()
            .name("repository")
            .value("local")
            .type(stringType)
            .build())
        .argument(GraphQLArgument.newArgument()
            .name("queryRef")
            .value("test-iri")
            .type(stringType)
            .build())
        .build();

    Map<String, Object> environmentArguments = Collections.singletonMap("subject", value);

    when(graphQlFieldDefinitionMock.getName()).thenReturn("breweries_with_query_ref_as_iri");
    when(graphQlFieldDefinitionMock.getType()).thenReturn(graphqlOutputType);
    when(graphQlFieldDefinitionMock.getDirective(Rdf4jDirectives.SPARQL_NAME)).thenReturn(directive);
    when(graphQlFieldDefinitionMock.getArguments()).thenReturn(arguments);

    when(dataFetchingEnvironmentMock.getFieldDefinition()).thenReturn(graphQlFieldDefinitionMock);
    when(dataFetchingEnvironmentMock.getArguments()).thenReturn(environmentArguments);

    when(tupleQueryResultMock.hasNext()).thenReturn(true)
        .thenReturn(false);
    when(tupleQueryResultMock.next()).thenReturn(EmptyBindingSet.getInstance());

    when(tupleQueryMock.evaluate()).thenReturn(tupleQueryResultMock);

    when(graphQueryResultMock.hasNext()).thenReturn(true)
        .thenReturn(false);

    Resource subject = SimpleValueFactory.getInstance()
        .createIRI("http://subject");
    IRI predicate = SimpleValueFactory.getInstance()
        .createIRI("http://predicate");
    Value object = SimpleValueFactory.getInstance()
        .createLiteral("object");

    when(graphQueryResultMock.next()).thenReturn(SimpleValueFactory.getInstance()
        .createStatement(subject, predicate, object));

    when(graphQueryMock.evaluate()).thenReturn(graphQueryResultMock);

    when(mockRepositoryAdapterMock.prepareTupleQuery(eq("local"), any(DataFetchingEnvironment.class), eq(SELECT_QUERY)))
        .thenReturn(tupleQueryMock);

    when(mockRepositoryAdapterMock.prepareGraphQuery(eq("local"), any(DataFetchingEnvironment.class), eq(SELECT_QUERY),
        anyList())).thenReturn(graphQueryMock);
  }

  @ParameterizedTest
  @MethodSource("createGraphqlOutputTypes")
  void testSupports(GraphQLOutputType graphqlOutputType) {
    // Arrange
    when(graphQlFieldDefinitionMock.getType()).thenReturn(graphqlOutputType);

    // Act
    boolean result = StaticQueryFetcher.supports(graphQlFieldDefinitionMock);

    // Assert
    assertTrue(result);
  }

  private static Stream<Arguments> createGraphqlOutputTypes() {
    // Arrange
    return Stream.of(Arguments.of(Rdf4jScalars.IRI), Arguments.of(Rdf4jScalars.MODEL),
        Arguments.of(Rdf4jScalars.SPARQL_QUERY_RESULT));
  }
}
