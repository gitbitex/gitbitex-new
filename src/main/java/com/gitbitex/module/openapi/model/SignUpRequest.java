package com.gitbitex.module.openapi.model;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequest {
    @Email
    @NotBlank(message = "Email cannot be empty")
    private String email;

    @NotBlank
    private String password;

    private String code;
}
