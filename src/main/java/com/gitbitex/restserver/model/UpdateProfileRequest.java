package com.gitbitex.restserver.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    private String nickName;
    private String twoStepVerificationType;
}
