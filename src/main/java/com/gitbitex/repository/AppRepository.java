package com.gitbitex.repository;

import com.gitbitex.entity.App;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AppRepository extends JpaRepository<App, Long>, CrudRepository<App, Long>,
        JpaSpecificationExecutor<App> {

    List<App> findByUserId(String userId);

    App findByAppId(String appId);

}
