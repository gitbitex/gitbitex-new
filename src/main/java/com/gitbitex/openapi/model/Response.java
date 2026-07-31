package com.gitbitex.openapi.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Response<T> {
    private T data;

    public Response(T data) {
        this.data = data;
    }
    
    public static <T> Response<T> success() {
        return new Response<>(null);
    }
    
    public static <T> Response<T> success(T data) {
        return new Response<>(data);
    }
}
