package com.gitbitex.openapi.controller;

import com.gitbitex.marketdata.entity.App;
import com.gitbitex.marketdata.entity.User;
import com.gitbitex.marketdata.repository.AppRepository;
import com.gitbitex.openapi.model.AppDto;
import com.gitbitex.openapi.model.CreateAppRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppController {
    private final AppRepository appRepository;

    @GetMapping("/apps")
    public List<AppDto> getApps(@RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        List<App> apps = appRepository.findByUserId(currentUser.getId());
        return apps.stream().map(this::appDto).collect(Collectors.toList());
    }

    @PostMapping("/apps")
    public AppDto createApp(CreateAppRequest request, @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        App app = new App();
        app.setId(UUID.randomUUID().toString());
        app.setUserId(currentUser.getId());
        app.setAccessKey(UUID.randomUUID().toString());
        app.setSecretKey(UUID.randomUUID().toString());
        app.setName(request.getName());
        appRepository.save(app);

        return appDto(app);
    }

    @DeleteMapping("/apps/{appId}")
    public void deleteApp(@PathVariable String appId, @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        App app = appRepository.findByAppId(appId);
        if (app == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!app.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        appRepository.deleteById(app.getId());
    }

    private AppDto appDto(App app) {
        AppDto appDto = new AppDto();
        appDto.setId(app.getId());
        appDto.setName(app.getName());
        appDto.setKey(app.getAccessKey());
        appDto.setSecret(app.getSecretKey());
        appDto.setCreatedAt(app.getCreatedAt() != null ? app.getCreatedAt().toInstant().toString() : null);
        return appDto;
    }
}
