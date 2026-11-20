package com.example.Social_Media_RestApi_Project.common.dto;

import com.example.Social_Media_RestApi_Project.exception.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Restricts client-provided sort parameters to an allow-list. Without it, Spring Data would accept
 * nested paths such as {@code sort=author.email} or {@code sort=password}, letting clients infer
 * the ordering of private fields.
 */
public final class SortValidator {

    private SortValidator() {
    }

    public static Pageable requireAllowed(Pageable pageable, Set<String> allowedProperties) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new BadRequestException("Sorting by '%s' is not supported. Allowed: %s"
                        .formatted(order.getProperty(), allowedProperties));
            }
        }
        return pageable;
    }
}
