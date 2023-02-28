package com.gitbitex.marketdata.repository;

import java.util.Collection;

import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.openapi.model.PagedList;
import org.springframework.stereotype.Component;

@Component
public class CandleRepository {

    public Candle findById(String id){
        return null;
    }

    public Candle findTopByProductIdAndGranularityOrderByTimeDesc(String productId, int granularity) {
        return null;
    }

    public PagedList<Candle> findAll(String productId, Integer granularity, int pageIndex, int pageSize) {
        return null;




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

    public void saveAll(Collection<Candle> candles) {}
}

