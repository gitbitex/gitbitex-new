package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.Candle;
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

public interface CandleRepository extends JpaRepository<Candle, Long>, CrudRepository<Candle, Long>,
        JpaSpecificationExecutor<Candle> {

    Candle findTopByProductIdAndGranularityOrderByTimeDesc(String productId, int granularity);

    default Page<Candle> findAll(String productId, Integer granularity, int pageIndex, int pageSize) {
        Specification<Candle> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (granularity != null) {
                predicates.add(cb.equal(root.get("granularity"), granularity));
            }
            if (productId != null) {
                predicates.add(cb.equal(root.get("productId"), productId));
            }
            return cb.and(predicates.toArray(new Predicate[]{}));
        };

        Pageable pager = PageRequest.of(pageIndex - 1, pageSize, Sort.by("time").descending());

        return this.findAll(specification, pager);
    }

}

