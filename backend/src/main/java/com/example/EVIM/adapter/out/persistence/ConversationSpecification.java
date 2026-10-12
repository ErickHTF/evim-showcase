package com.example.EVIM.adapter.out.persistence;

import com.example.EVIM.adapter.out.persistence.entity.ConversationJpaEntity;
import com.example.EVIM.domain.model.Status;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ConversationSpecification {

    public static Specification<ConversationJpaEntity> getFilter(Status status, Long ticketId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (ticketId != null) {
                predicates.add(criteriaBuilder.equal(root.get("ticketId"), ticketId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
