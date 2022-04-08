package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenDto {
    private String token;
    private String twoStepVerification;
}
