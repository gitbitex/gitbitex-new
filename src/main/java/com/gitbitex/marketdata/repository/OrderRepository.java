package com.gitbitex.marketdata.repository;

import com.gitbitex.enums.OrderSide;
import com.gitbitex.enums.OrderStatus;
import com.gitbitex.marketdata.entity.Order;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface OrderRepository extends MongoRepository<Order, String>{

    Order findByOrderId(String orderId);

    default Page<Order> findAll(String userId, String productId, OrderStatus status, OrderSide side,
        int pageIndex, int pageSize) {

        Order user = new Order();
        if (userId!=null) {
            user.setUserId(userId);
        }
        if (productId!=null){
            user.setProductId(productId);
        }
        if (status!=null){
            user.setStatus(status);
        }


        ExampleMatcher matcher = ExampleMatcher.matching().withIgnorePaths("age", "createTime");
        Example<Order> example = Example.of(user, matcher);

        PageRequest pageable = PageRequest.of(pageIndex - 1, pageSize);
        return this.findAll(example, pageable);






       /* Specification<Order> specification = (root, query, cb) -> {
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
            return cb.and(predicates.toArray(new Predicate[] {}));
        };

        Pageable pager = PageRequest.of(pageIndex - 1, pageSize, Sort.by("id").descending());

        return this.findAll(specification, pager);*/
    }
}
