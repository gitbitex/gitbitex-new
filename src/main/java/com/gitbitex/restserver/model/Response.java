package com.gitbitex.restserver.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Response<T> {
    private T data;

    public Response(T data) {
        this.data = data;
    }
}
