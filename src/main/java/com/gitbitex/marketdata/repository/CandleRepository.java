package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.marketdata.entity.Order;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.CrudRepository;

public interface CandleRepository extends MongoRepository<Candle, Long>, CrudRepository<Candle, Long> {

    Candle findTopByProductIdAndGranularityOrderByTimeDesc(String productId, int granularity);

    default Page<Candle> findAll(String productId, Integer granularity, int pageIndex, int pageSize) {
        Candle user = new Candle();
        if (granularity!=null) {
            user.setGranularity(granularity);
        }
        if (productId!=null){
            user.setProductId(productId);
        }


        ExampleMatcher matcher = ExampleMatcher.matching().withIgnorePaths("age", "createTime");
        Example<Candle> example = Example.of(user, matcher);

        PageRequest pageable = PageRequest.of(pageIndex - 1, pageSize);
        return this.findAll(example, pageable);




        /*Specification<Candle> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (granularity != null) {
                predicates.add(cb.equal(root.get("granularity"), granularity));
            }
            if (productId != null) {
                predicates.add(cb.equal(root.get("productId"), productId));
            }
            return cb.and(predicates.toArray(new Predicate[] {}));
        };

        Pageable pager = PageRequest.of(pageIndex - 1, pageSize, Sort.by("time").descending());

        return this.findAll(specification, pager);*/
    }

}

