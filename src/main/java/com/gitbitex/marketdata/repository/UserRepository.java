package com.gitbitex.marketdata.repository;

import com.gitbitex.marketdata.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserRepository {

    public User findByEmail(String email) {
        return null;
    }

    public User findByUserId(String userId) {
        return null;
    }

    public void save(User user) {

    }

}
