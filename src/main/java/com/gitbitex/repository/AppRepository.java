package com.gitbitex.repository;

import java.util.List;

import com.gitbitex.entity.App;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface AppRepository extends JpaRepository<App, Long>, CrudRepository<App, Long>,
    JpaSpecificationExecutor<App> {

    List<App> findByUserId(String userId);

    App findByAppId(String appId);

}
