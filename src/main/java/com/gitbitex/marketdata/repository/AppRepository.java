package com.gitbitex.marketdata.repository;

import java.util.List;

import com.gitbitex.marketdata.entity.App;
import org.springframework.stereotype.Component;

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
