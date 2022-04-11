package com.gitbitex.openapi.controller;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.gitbitex.openapi.model.SignInRequest;
import com.gitbitex.openapi.model.SignUpRequest;
import com.gitbitex.openapi.model.TokenDto;
import com.gitbitex.openapi.model.UpdateProfileRequest;
import com.gitbitex.openapi.model.UserDto;
import com.gitbitex.user.UserManager;
import com.gitbitex.user.entity.User;
import com.gitbitex.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserManager userManager;
    private final UserRepository userRepository;

    @GetMapping("/users/self")
    public UserDto getCurrentUser(@RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userDto(currentUser);
    }

    @PutMapping("/users/self")
    public UserDto updateProfile(@RequestBody UpdateProfileRequest updateProfileRequest,
        @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        if (updateProfileRequest.getNickName() != null) {
            currentUser.setNickName(updateProfileRequest.getNickName());
        }
        if (updateProfileRequest.getTwoStepVerificationType() != null) {
            currentUser.setTwoStepVerificationType(updateProfileRequest.getTwoStepVerificationType());
        }
        userRepository.save(currentUser);

        return userDto(currentUser);
    }

    @PostMapping("/users/accessToken")
    public TokenDto signIn(@RequestBody @Valid SignInRequest signInRequest, HttpServletRequest request,
        HttpServletResponse response) {
        User user = userManager.getUser(signInRequest.getEmail(), signInRequest.getPassword());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "email or password error");
        }

        String token = userManager.generateAccessToken(user, request.getSession().getId());

        addAccessTokenCookie(response, token);

        TokenDto tokenDto = new TokenDto();
        tokenDto.setToken(token);
        tokenDto.setTwoStepVerification("none");
        return tokenDto;
    }

    @DeleteMapping("/users/accessToken")
    public void signOut(@RequestAttribute(required = false) User currentUser,
        @RequestAttribute(required = false) String accessToken) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        userManager.deleteAccessToken(accessToken);
    }

    @PostMapping("/users")
    public UserDto signUp(@RequestBody @Valid SignUpRequest signUpRequest) {
        User user = userManager.createUser(signUpRequest.getEmail(), signUpRequest.getPassword());
        return userDto(user);
    }

    private void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie cookie = new Cookie("accessToken", accessToken);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        cookie.setSecure(false);
        cookie.setHttpOnly(false);
        response.addCookie(cookie);
    }

    private UserDto userDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getUserId());
        userDto.setEmail(user.getEmail());
        userDto.setBand(false);
        userDto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toInstant().toString() : null);
        userDto.setName(user.getNickName());
        userDto.setTwoStepVerificationType(user.getTwoStepVerificationType());
        return userDto;
    }
}
