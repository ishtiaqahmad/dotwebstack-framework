package org.dotwebstack.framework.core.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

public interface CoreDataFetcher<T> extends DataFetcher<T> {

  boolean supports(DataFetchingEnvironment environment);

  DataFetcherType getType();

}
