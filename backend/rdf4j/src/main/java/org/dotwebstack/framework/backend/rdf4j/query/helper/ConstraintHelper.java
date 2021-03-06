package org.dotwebstack.framework.backend.rdf4j.query.helper;

import static org.dotwebstack.framework.backend.rdf4j.helper.IriHelper.stringify;
import static org.dotwebstack.framework.backend.rdf4j.query.helper.EdgeHelper.buildEdge;
import static org.dotwebstack.framework.backend.rdf4j.query.helper.EdgeHelper.findExistingEdge;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.dotwebstack.framework.backend.rdf4j.query.model.Constraint;
import org.dotwebstack.framework.backend.rdf4j.query.model.Edge;
import org.dotwebstack.framework.backend.rdf4j.query.model.PathType;
import org.dotwebstack.framework.backend.rdf4j.query.model.QueryType;
import org.dotwebstack.framework.backend.rdf4j.query.model.Vertice;
import org.dotwebstack.framework.backend.rdf4j.shacl.ConstraintType;
import org.dotwebstack.framework.backend.rdf4j.shacl.NodeShape;
import org.dotwebstack.framework.backend.rdf4j.shacl.PropertyShape;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sparqlbuilder.core.query.OuterQuery;

public class ConstraintHelper {

  private ConstraintHelper() {}

  public static void addResolvedRequiredEdges(Vertice vertice, Collection<PropertyShape> propertyShapes,
      OuterQuery<?> query) {
    propertyShapes.stream()
        .filter(ps -> ps.getMinCount() != null && ps.getMinCount() >= 1 && ps.getNode() != null)
        .forEach(ps -> {
          Edge edge = findExistingEdge(vertice, ps, PathType.CONSTRAINT)
              .orElseGet(() -> buildEdge(query.var(), ps, PathType.CONSTRAINT));
          edge.addPathType(PathType.CONSTRAINT);

          vertice.getEdges()
              .add(edge);
          addResolvedRequiredEdges(edge.getObject(), ps.getNode()
              .getPropertyShapes()
              .values(), query);
        });
  }

  public static List<Edge> resolveRequiredEdges(Collection<PropertyShape> propertyShapes, OuterQuery<?> query) {
    return propertyShapes.stream()
        .filter(ps -> ps.getMinCount() != null && ps.getMinCount() >= 1)
        .map(ps -> {
          Edge edge = buildEdge(query.var(), ps, PathType.CONSTRAINT);
          if (ps.getNode() != null) {
            addResolvedRequiredEdges(edge.getObject(), ps.getNode()
                .getPropertyShapes()
                .values(), query);
          }
          return edge;
        })
        .collect(Collectors.toList());
  }

  /*
   * Find out if given edge contains a child edge of given type.
   */
  static boolean hasConstraintOfType(Vertice vertice, Set<Set<IRI>> orTypes) {
    return orTypes.stream()
        .anyMatch(andTypes -> andTypes.stream()
            .allMatch(type -> vertice.getConstraints(ConstraintType.RDF_TYPE)
                .stream()
                .flatMap(constraint -> constraint.getValues()
                    .stream())
                .anyMatch(value -> value.equals(type))));
  }

  public static Optional<Constraint> buildTypeConstraint(NodeShape nodeShape) {
    Set<Object> classes = new HashSet<>(nodeShape.getClasses());
    if (!classes.isEmpty()) {
      return Optional.of(Constraint.builder()
          .constraintType(ConstraintType.RDF_TYPE)
          .predicate(() -> stringify(RDF.TYPE))
          .values(classes)
          .build());
    }
    return Optional.empty();
  }

  public static Optional<Constraint> buildValueConstraint(PropertyShape propertyShape) {
    if (propertyShape.getMinCount() != null && propertyShape.getMinCount() >= 1
        && propertyShape.getHasValue() != null) {
      return Optional.of(Constraint.builder()
          .predicate(propertyShape.getPath()
              .toPredicate())
          .constraintType(ConstraintType.HASVALUE)
          .values(Set.of(propertyShape.getHasValue()))
          .build());
    } else {
      return Optional.empty();
    }
  }

  /*
   * Check which edges should be added to the where part of the query based on a sh:minCount property
   * of 1
   */
  public static void buildConstraints(@NonNull Vertice vertice, QueryType queryType,
      @NonNull OuterQuery<?> outerQuery) {
    buildTypeConstraint(vertice.getNodeShape()).ifPresent(constraint -> {
      vertice.addConstraint(constraint);

      // add an edge to be able to query the types in the construct part
      if (Objects.equals(QueryType.CONSTRUCT, queryType) && !vertice.hasTypeEdge()) {
        vertice.addEdge(buildEdge(outerQuery.var(), PathType.CONSTRAINT));
      }
    });
    vertice.getNodeShape()
        .getPropertyShapes()
        .values()
        .forEach(ps -> buildValueConstraint(ps).ifPresent(vertice.getConstraints()::add));

    vertice.getEdges()
        .stream()
        .filter(edge -> edge.getPropertyShape() != null)
        .forEach(edge -> {
          Vertice childVertice = edge.getObject();
          NodeShape childNodeShape = childVertice.getNodeShape();
          if (childNodeShape != null) {
            buildConstraints(childVertice, queryType, outerQuery);
          }
        });
  }
}
