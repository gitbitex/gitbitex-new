package com.gitbitex.openapi.controller;

import com.gitbitex.marketdata.entity.AppEntity;
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

        List<AppEntity> appEntities = appRepository.findByUserId(currentUser.getId());
        return appEntities.stream().map(this::appDto).collect(Collectors.toList());
    }

    @PostMapping("/apps")
    public AppDto createApp(CreateAppRequest request, @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        AppEntity appEntity = new AppEntity();
        appEntity.setId(UUID.randomUUID().toString());
        appEntity.setUserId(currentUser.getId());
        appEntity.setAccessKey(UUID.randomUUID().toString());
        appEntity.setSecretKey(UUID.randomUUID().toString());
        appEntity.setName(request.getName());
        appRepository.save(appEntity);

        return appDto(appEntity);
    }

    @DeleteMapping("/apps/{appId}")
    public void deleteApp(@PathVariable String appId, @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        AppEntity appEntity = appRepository.findByAppId(appId);
        if (appEntity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!appEntity.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        appRepository.deleteById(appEntity.getId());
    }

    private AppDto appDto(AppEntity appEntity) {
        AppDto appDto = new AppDto();
        appDto.setId(appEntity.getId());
        appDto.setName(appEntity.getName());
        appDto.setKey(appEntity.getAccessKey());
        appDto.setSecret(appEntity.getSecretKey());
        appDto.setCreatedAt(appEntity.getCreatedAt() != null ? appEntity.getCreatedAt().toInstant().toString() : null);
        return appDto;
    }
}
