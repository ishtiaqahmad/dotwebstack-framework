package org.dotwebstack.framework.backend.rdf4j.query.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.dotwebstack.framework.backend.rdf4j.shacl.ConstraintType;
import org.dotwebstack.framework.backend.rdf4j.shacl.NodeShape;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EdgeHelperTest {

  private static final ValueFactory VF = SimpleValueFactory.getInstance();

  @Test
  void hasEqualTargetClass_returnsTrue_withNullNodeShape() {
    IRI iri = VF.createIRI("http://www.example.com#testType");

    Vertice vertice = Vertice.builder()
        .constraints(Set.of(Constraint.builder()
            .constraintType(ConstraintType.RDF_TYPE)
            .predicate(() -> "<" + RDF.TYPE.stringValue() + ">")
            .values(Set.of(iri))
            .build()))
        .build();
    NodeShape shape = NodeShape.builder()
        .classes(Set.of(VF.createIRI("http://www.example.com#testType")))
        .build();

    assertTrue(EdgeHelper.hasEqualTargetClass(vertice, shape));
  }

  @Test
  void hasEqualTargetClass_returnsFalse_forChildWithoutSameType() {
    Vertice vertice = Vertice.builder()
        .constraints(Set.of(Constraint.builder()
            .constraintType(ConstraintType.RDF_TYPE)
            .predicate(() -> "<" + RDF.TYPE.stringValue() + ">")
            .values(Set.of(VF.createIRI("http://www.example.com#otherType")))
            .build()))
        .build();
    NodeShape shape = NodeShape.builder()
        .classes(Set.of(VF.createIRI("http://www.example.com#testType")))
        .build();

    assertFalse(EdgeHelper.hasEqualTargetClass(vertice, shape));
  }

  @Test
  public void hasEqualTargetClass_returnsTrue_forNullNodeShape() {
    // Arrange / Act / Assert
    assertTrue(EdgeHelper.hasEqualTargetClass(null, null));
  }

  @Test
  public void deepList_returns_listOfEdges() {
    // Arrange
    Edge edge1 = Edge.builder()
        .object(Vertice.builder()
            .edges(Collections.emptyList())
            .build())
        .build();
    Edge edge2 = Edge.builder()
        .object(Vertice.builder()
            .edges(Collections.singletonList(edge1))
            .build())
        .build();
    Edge edge3 = Edge.builder()
        .object(Vertice.builder()
            .edges(Collections.singletonList(edge2))
            .build())
        .build();

    // Act
    List<Edge> edges = EdgeHelper.deepList(Collections.singletonList(edge3));

    // Assert
    assertEquals(3, edges.size());
    assertTrue(edges.contains(edge1));
    assertTrue(edges.contains(edge2));
    assertTrue(edges.contains(edge3));
  }
}
