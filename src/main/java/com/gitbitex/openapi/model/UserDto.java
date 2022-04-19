package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    private String id;
    private String email;
    private String name;
    private String profilePhoto;
    private boolean isBand;
    private String createdAt;
    private String twoStepVerificationType;
}
