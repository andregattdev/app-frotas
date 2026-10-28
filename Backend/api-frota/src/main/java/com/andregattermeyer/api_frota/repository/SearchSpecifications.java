package com.andregattermeyer.api_frota.repository;

import java.util.Arrays;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

public final class SearchSpecifications {

    private SearchSpecifications() {
    }

    public static <T> Specification<T> containsAnyIgnoreCase(String term, String... fields) {
        if (term == null || term.isBlank()) {
            return (root, query, builder) -> builder.conjunction();
        }

        return (root, query, builder) -> builder.or(Arrays.stream(fields)
                .map(field -> containsIgnoreCase(builder, root.get(field).as(String.class), term))
                .toArray(Predicate[]::new));
    }

    public static Predicate containsIgnoreCase(
            CriteriaBuilder builder, Expression<String> expression, String term) {
        String escapedTerm = term.toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return builder.like(builder.lower(expression), "%" + escapedTerm + "%", '!');
    }
}
