package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.App;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppRepository {

    public List<App> findByUserId(String userId) {
        return null;
    }

    public App findByAppId(String appId) {
        return null;
    }

    public void save(App app) {

    }

    public void deleteById(String id) {

    }
}
