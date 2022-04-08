package com.gitbitex.repository;

import com.gitbitex.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends JpaRepository<User, Long>, CrudRepository<User, Long>,
    JpaSpecificationExecutor<User> {

    User findByEmail(String email);

    User findByUserId(String userId);

}
