package com.gitbitex.marketdata.manager;

import com.gitbitex.marketdata.entity.TradeEntity;
import com.gitbitex.marketdata.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeManager {
    private final TradeRepository tradeRepository;

    public void saveAll(Collection<TradeEntity> trades) {
        if (trades.isEmpty()) {
            return;
        }

        long t1 = System.currentTimeMillis();
        tradeRepository.saveAll(trades);
        logger.info("saved {} trade(s) ({}ms)", trades.size(), System.currentTimeMillis() - t1);
    }
}

