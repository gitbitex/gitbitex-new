package com.gitbitex.order.repository;

import com.gitbitex.order.entity.Order;
import com.gitbitex.order.entity.Order.OrderSide;
import com.gitbitex.order.entity.Order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>, CrudRepository<Order, Long>,
        JpaSpecificationExecutor<Order> {

    Order findByOrderId(String orderId);

    default Page<Order> findAll(String userId, String productId, OrderStatus status, OrderSide side,
                                int pageIndex, int pageSize) {
        Specification<Order> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (productId != null) {
                predicates.add(cb.equal(root.get("productId"), productId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (side != null) {
                predicates.add(cb.equal(root.get("side"), side));
            }
            return cb.and(predicates.toArray(new Predicate[]{}));
        };

        Pageable pager = PageRequest.of(pageIndex - 1, pageSize, Sort.by("id").descending());

        return this.findAll(specification, pager);
    }
}
