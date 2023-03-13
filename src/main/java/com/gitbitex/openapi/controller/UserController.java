package com.gitbitex.openapi.controller;

import com.gitbitex.marketdata.entity.User;
import com.gitbitex.marketdata.manager.UserManager;
import com.gitbitex.marketdata.repository.UserRepository;
import com.gitbitex.matchingengine.command.DepositCommand;
import com.gitbitex.matchingengine.command.MatchingEngineCommandProducer;
import com.gitbitex.openapi.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserManager userManager;
    private final UserRepository userRepository;
    private final MatchingEngineCommandProducer matchingEngineCommandProducer;

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

        //TODO: Recharge each user for demonstration
        deposit(user.getId(), "BTC", BigDecimal.valueOf(1000000000));
        deposit(user.getId(), "ETH", BigDecimal.valueOf(1000000000));
        deposit(user.getId(), "USDT", BigDecimal.valueOf(1000000000));

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
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setBand(false);
        userDto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toInstant().toString() : null);
        userDto.setName(user.getNickName());
        userDto.setTwoStepVerificationType(user.getTwoStepVerificationType());
        return userDto;
    }

    private void deposit(String userId, String currency, BigDecimal amount) {
        DepositCommand command = new DepositCommand();
        command.setUserId(userId);
        command.setCurrency(currency);
        command.setAmount(amount);
        command.setTransactionId(UUID.randomUUID().toString());
        matchingEngineCommandProducer.send(command, null);
    }
}
