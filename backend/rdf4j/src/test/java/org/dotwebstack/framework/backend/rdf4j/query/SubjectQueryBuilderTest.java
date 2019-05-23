package org.dotwebstack.framework.backend.rdf4j.query;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLObjectType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.MapContext;
import org.dotwebstack.framework.backend.rdf4j.directives.Rdf4jDirectives;
import org.dotwebstack.framework.backend.rdf4j.shacl.NodeShape;
import org.dotwebstack.framework.backend.rdf4j.shacl.NodeShapeRegistry;
import org.dotwebstack.framework.backend.rdf4j.shacl.PropertyShape;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubjectQueryBuilderTest {

  private final JexlEngine jexlEngine = new JexlBuilder().silent(false)
      .strict(true)
      .create();

  @Mock
  private QueryEnvironment environmentMock = mock(QueryEnvironment.class);

  @Mock
  private NodeShapeRegistry registryMock = mock(NodeShapeRegistry.class);

  @Mock
  private NodeShape nodeShapeMock = mock(NodeShape.class);

  @Mock
  private PropertyShape propertyShapeMock = mock(PropertyShape.class);

  @Mock
  private GraphQLObjectType objectTypeMock = mock(GraphQLObjectType.class);

  private SubjectQueryBuilder subjectQueryBuilder;

  @BeforeEach
  void setUp() {
    when(this.environmentMock.getNodeShapeRegistry()).thenReturn(this.registryMock);
    when(this.environmentMock.getObjectType()).thenReturn(this.objectTypeMock);
    when(this.environmentMock.getNodeShapeRegistry()
        .get(any(GraphQLObjectType.class))).thenReturn(this.nodeShapeMock);
    this.subjectQueryBuilder = SubjectQueryBuilder.create(this.environmentMock, this.jexlEngine);
  }

  private GraphQLDirective getValidPagingDirective() {
    return GraphQLDirective.newDirective()
        .name("sparql")
        .argument(GraphQLArgument.newArgument()
            .name(Rdf4jDirectives.SPARQL_ARG_OFFSET)
            .type(Scalars.GraphQLString)
            .value("(page - 1) * pageSize")
            .build())
        .argument(GraphQLArgument.newArgument()
            .name(Rdf4jDirectives.SPARQL_ARG_LIMIT)
            .type(Scalars.GraphQLString)
            .value("pageSize")
            .build())
        .build();
  }

  private GraphQLDirective getValidSortDirective() {
    return GraphQLDirective.newDirective()
        .name("sparql")
        .argument(GraphQLArgument.newArgument()
            .name(Rdf4jDirectives.SPARQL_ARG_ORDER_BY)
            .type(Scalars.GraphQLString)
            .value("sort")
            .build())
        .build();
  }

  private GraphQLDirective getInvalidPagingDirective() {
    return GraphQLDirective.newDirective()
        .name("sparql")
        .argument(GraphQLArgument.newArgument()
            .name(Rdf4jDirectives.SPARQL_ARG_OFFSET)
            .type(Scalars.GraphQLString)
            .value("this is an invalid expression")
            .build())
        .argument(GraphQLArgument.newArgument()
            .name(Rdf4jDirectives.SPARQL_ARG_LIMIT)
            .type(Scalars.GraphQLString)
            .value("this is an invalid expression")
            .build())
        .build();
  }

  @Test
  void test_pagingParameters_whenNoExpressionsSet() {
    GraphQLDirective emptySparqlDirective = GraphQLDirective.newDirective()
        .name("sparql")
        .build();

    Map<String, Object> arguments = new HashMap<>();
    arguments.put("page", 1);
    arguments.put("pageSize", 12);

    MapContext context = new MapContext(arguments);
    assertThat(this.subjectQueryBuilder.getLimitFromContext(context, emptySparqlDirective),
        is(equalTo(Optional.empty())));
    assertThat(this.subjectQueryBuilder.getOffsetFromContext(context, emptySparqlDirective),
        is(equalTo(Optional.empty())));
  }

  @Test
  void test_pagingParameters_whenNoValuesSet() {
    GraphQLDirective validSparqlDirective = getValidPagingDirective();
    Map<String, Object> arguments = new HashMap<>();

    MapContext context = new MapContext(arguments);

    assertThrows(org.apache.commons.jexl3.JexlException.Variable.class,
        () -> this.subjectQueryBuilder.getLimitFromContext(context, validSparqlDirective));
    assertThrows(org.apache.commons.jexl3.JexlException.Variable.class,
        () -> this.subjectQueryBuilder.getOffsetFromContext(context, validSparqlDirective));
  }

  @Test
  void test_pagingParameters_withValidPageAndPageSize() {
    GraphQLDirective validSparqlDirective = getValidPagingDirective();
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("page", 5);
    arguments.put("pageSize", 12);

    MapContext context = new MapContext(arguments);

    assertThat(this.subjectQueryBuilder.getLimitFromContext(context, validSparqlDirective),
        is(equalTo(Optional.of(12))));
    assertThat(this.subjectQueryBuilder.getOffsetFromContext(context, validSparqlDirective),
        is(equalTo(Optional.of(48))));
  }

  @Test
  void test_pagingParameters_withInvalidPage() {
    GraphQLDirective validSparqlDirective = getValidPagingDirective();
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("page", -1);
    arguments.put("pageSize", 12);

    MapContext context = new MapContext(arguments);

    assertThat(this.subjectQueryBuilder.getLimitFromContext(context, validSparqlDirective),
        is(equalTo(Optional.of(12))));
    assertThrows(IllegalArgumentException.class,
        () -> this.subjectQueryBuilder.getOffsetFromContext(context, validSparqlDirective));
  }

  @Test
  void test_pagingParameters_withInvalidPageType() {
    GraphQLDirective validSparqlDirective = getValidPagingDirective();
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("page", "test");
    arguments.put("pageSize", 12);

    MapContext context = new MapContext(arguments);

    assertThat(this.subjectQueryBuilder.getLimitFromContext(context, validSparqlDirective),
        is(equalTo(Optional.of(12))));
    assertThrows(IllegalArgumentException.class,
        () -> this.subjectQueryBuilder.getOffsetFromContext(context, validSparqlDirective));
  }

  @Test
  void test_pagingParameters_withInvalidPageSize() {
    GraphQLDirective validSparqlDirective = getValidPagingDirective();
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("page", 13);
    arguments.put("pageSize", -1);

    MapContext context = new MapContext(arguments);

    assertThrows(IllegalArgumentException.class,
        () -> this.subjectQueryBuilder.getLimitFromContext(context, validSparqlDirective));
    assertThrows(IllegalArgumentException.class,
        () -> this.subjectQueryBuilder.getOffsetFromContext(context, validSparqlDirective));
  }

  @Test
  void test_pagingParameters_withInvalidPageSizeType() {
    GraphQLDirective validSparqlDirective = getValidPagingDirective();
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("page", 13);
    arguments.put("pageSize", "test");

    MapContext context = new MapContext(arguments);

    assertThrows(IllegalArgumentException.class,
        () -> this.subjectQueryBuilder.getLimitFromContext(context, validSparqlDirective));
    assertThrows(IllegalArgumentException.class,
        () -> this.subjectQueryBuilder.getOffsetFromContext(context, validSparqlDirective));
  }

  @Test
  void test_pagingParameters_withInvalidLimitAndOffsetExpressions() {
    GraphQLDirective invalidSparqlDirective = getInvalidPagingDirective();

    Map<String, Object> arguments = new HashMap<>();
    arguments.put("page", 1);
    arguments.put("pageSize", 5);

    MapContext context = new MapContext(arguments);

    assertThrows(org.apache.commons.jexl3.JexlException.Parsing.class,
        () -> this.subjectQueryBuilder.getLimitFromContext(context, invalidSparqlDirective));
    assertThrows(org.apache.commons.jexl3.JexlException.Parsing.class,
        () -> this.subjectQueryBuilder.getOffsetFromContext(context, invalidSparqlDirective));
  }

  @Test
  void test_sortCondition_withValidOrderByExpressions() {
    when(this.nodeShapeMock.getPropertyShape(any(String.class))).thenReturn(propertyShapeMock);

    GraphQLDirective validSparqlDirective = getValidSortDirective();

    Map<String, Object> arguments = new HashMap<>();
    arguments.put("sort", ImmutableList.of(ImmutableMap.of("field", "identifier", "order", "DESC")));

    MapContext context = new MapContext(arguments);
    Optional<List<OrderContext>> optional =
        this.subjectQueryBuilder.getOrderByFromContext(context, validSparqlDirective);

    assertThat(optional.isPresent(), is(true));

    optional.ifPresent(orderContexts -> assertThat(orderContexts.get(0)
        .getOrderable()
        .getQueryString(), is(equalTo("DESC( ?identifier )"))));
  }

  @Test
  void test_sortCondition_withoutSortProperty() {
    GraphQLDirective validSparqlDirective = getValidSortDirective();
    Map<String, Object> arguments = new HashMap<>();

    MapContext context = new MapContext(arguments);
    assertThrows(JexlException.Variable.class,
        () -> this.subjectQueryBuilder.getOrderByFromContext(context, validSparqlDirective));
  }

  @Test
  void test_sortingProperty_whenSortPropertyNotExistsOnNodeshape() {
    when(this.nodeShapeMock.getPropertyShape(any(String.class))).thenReturn(null);

    GraphQLDirective validSparqlDirective = getValidSortDirective();

    Map<String, Object> arguments = new HashMap<>();
    arguments.put("sort", ImmutableList.of(ImmutableMap.of("field", "identifier", "order", "DESC")));

    MapContext context = new MapContext(arguments);
    assertThrows(IllegalArgumentException.class,
        () -> this.subjectQueryBuilder.getOrderByFromContext(context, validSparqlDirective));
  }
}
