package com.hhm.graphql;

import com.hhm.service.ServiceException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof ServiceException se) {
            ErrorType errorType = switch (se.getType()) {
                case NOT_FOUND -> ErrorType.NOT_FOUND;
                case CONFLICT, VALIDATION -> ErrorType.BAD_REQUEST;
            };
            return GraphqlErrorBuilder.newError(env)
                    .message(se.getMessage())
                    .errorType(errorType)
                    .build();
        }

        if (ex instanceof ConstraintViolationException cve) {
            String message = cve.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            return GraphqlErrorBuilder.newError(env)
                    .message("Validation failed: " + message)
                    .errorType(ErrorType.BAD_REQUEST)
                    .build();
        }

        if (ex instanceof DataIntegrityViolationException) {
            return GraphqlErrorBuilder.newError(env)
                    .message("Data conflict: a record with one of the provided values already exists.")
                    .errorType(ErrorType.BAD_REQUEST)
                    .build();
        }

        return null;
    }
}
