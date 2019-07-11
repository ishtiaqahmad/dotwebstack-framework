package org.dotwebstack.framework.core.helpers;

import graphql.language.ListType;
import graphql.language.NonNullType;
import graphql.language.Type;
import graphql.language.TypeName;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;

public class TypeHelper {
  private TypeHelper() {}

  public static boolean hasListType(Type<?> type) {
    if (type instanceof NonNullType) {
      return hasListType(((NonNullType) type).getType());
    } else if (type instanceof ListType) {
      return true;
    } else if (type instanceof TypeName) {
      return false;
    } else {
      throw ExceptionHelper.illegalArgumentException("unsupported type: '{}'", type.getClass());
    }
  }

  public static Type<?> getBaseType(Type<?> type) {
    if (type instanceof ListType) {
      return getBaseType((Type<?>) type.getChildren()
          .get(0));
    }
    if (type instanceof NonNullType) {
      return getBaseType(((NonNullType) type).getType());
    }
    return type;
  }

  public static String getTypeName(Type<?> type) {
    if (type instanceof NonNullType) {
      return getTypeName(((NonNullType) type).getType());
    } else if (type instanceof ListType) {
      return getTypeName(((ListType) type).getType());
    } else if (type instanceof TypeName) {
      return ((TypeName) type).getName();
    } else {
      throw ExceptionHelper.illegalArgumentException("unsupported type: '{}'", type.getClass());
    }
  }

  public static String getTypeName(GraphQLType type) {
    if (type instanceof GraphQLList) {
      return getTypeName(((GraphQLList) type).getWrappedType());
    } else if (type instanceof GraphQLInputObjectType) {
      return type.getName();
    } else if (type instanceof GraphQLNonNull) {
      return getTypeName(((GraphQLNonNull) type).getWrappedType());
    } else if (type instanceof GraphQLObjectType) {
      return type.getName();
    } else if (type instanceof GraphQLScalarType) {
      return type.getName();
    } else {
      throw ExceptionHelper.illegalArgumentException("unsupported type: '{}'", type.getClass());
    }
  }
}
