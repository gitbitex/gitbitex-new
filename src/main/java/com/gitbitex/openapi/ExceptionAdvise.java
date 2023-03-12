package com.gitbitex.openapi;

import com.gitbitex.exception.ServiceException;
import com.gitbitex.openapi.model.ErrorMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author lingqingwan
 */
@ControllerAdvice
@Slf4j
public class ExceptionAdvise {
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ErrorMessage handleException(Exception e) {
        logger.error("http error", e);

        return new ErrorMessage(e.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public ErrorMessage handleException(ServiceException e, RequestContext request) {
        logger.error("http error: {} {} {}", e.getCode(), e.getMessage(), request.getRequestUri());

        return new ErrorMessage(e.getCode().name());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ErrorMessage handleException(MethodArgumentNotValidException e) {
        logger.error("http error", e);

        StringBuilder sb = new StringBuilder();
        e.getFieldErrors().forEach(x -> {
            sb.append(x.getField()).append(":").append(x.getDefaultMessage()).append("\n");
        });

        return new ErrorMessage(sb.toString());
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody
    public ErrorMessage handleException(ResponseStatusException e, HttpServletRequest request,
                                        HttpServletResponse response) {
        logger.error("http error: {} {}", e.getMessage(), request.getRequestURI());

        response.setStatus(e.getStatus().value());
        return new ErrorMessage(e.getMessage());
    }
}
