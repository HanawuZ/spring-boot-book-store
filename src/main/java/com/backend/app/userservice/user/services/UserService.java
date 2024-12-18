package com.backend.app.userservice.user.services;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties.Jwt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.backend.app.boostrapper.config.redis.RedisUtilities;
import com.backend.app.shared.libraries.http.BaseResponse;
import com.backend.app.shared.libraries.redis.RedisValueUtility;
import com.backend.app.shared.libraries.validator.PatternMatch;
import com.backend.app.shared.libraries.validator.ValidateValue;
import com.backend.app.shared.models.entities.Customer;
import com.backend.app.shared.models.entities.User;
import com.backend.app.shared.models.entities.UserMapping;
import com.backend.app.userservice.user.models.SignInRequest;
import com.backend.app.userservice.user.models.SignUpRequest;
import com.backend.app.userservice.user.repositories.UserRepository;
import com.backend.app.shared.libraries.security.authenticator.GoogleAuthenticatorService;
import com.backend.app.shared.libraries.security.jwt.JwtUtility;

interface UserServiceInterface {
  public BaseResponse<String> signIn(SignInRequest request);

  public BaseResponse<String> createUserFromSignUp(SignUpRequest request);
}

@Service
public class UserService implements UserServiceInterface {

  private PasswordEncoder passwordEncoder;
  private UserRepository userRepository;
  private GoogleAuthenticatorService googleAuthenticatorService;
  private JwtUtility jwtUtility;
  private RedisValueUtility redisValueUtility;
  private static final Integer MAX_LOGIN_ATTEMPT_LIMIT = 5;

  @Autowired
  public UserService(
      PasswordEncoder passwordEncoder,
      UserRepository userRepository,
      GoogleAuthenticatorService googleAuthenticatorService,
      JwtUtility jwtUtility,
      RedisValueUtility redisValueUtility) {
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
    this.googleAuthenticatorService = googleAuthenticatorService;
    this.jwtUtility = jwtUtility;
    this.redisValueUtility = redisValueUtility;
  }

  @Override
  public BaseResponse<String> signIn(SignInRequest request) {
    try {
      if (request == null) {
        return new BaseResponse<>(4000, "Empty request", null);
      }

      if ((request.getUsername() == null || request.getUsername().isEmpty()) &&
          (request.getEmail() == null || request.getEmail().isEmpty())) {
        return new BaseResponse<>(4000, "Username or email is required", null);
      }

      if (!request.getEmail().isEmpty() && !PatternMatch.isEmailValid(request.getEmail())) {
        return new BaseResponse<>(4000, "Email is not valid", null);
      }

      if (request.getPassword() == null || request.getPassword().isEmpty()) {
        return new BaseResponse<>(4000, "Password is required", null);
      }

      Optional<User> user = userRepository.findByUsernameOrEmail(request.getUsername(), request.getEmail());
      if (user.isEmpty()) {
        return new BaseResponse<>(4000, "User not found", null);
      }

      // Check if password is matched
      // TODO: Should blocked request from IP
      if (!passwordEncoder.matches(request.getPassword(), user.get().getPassword())) {
        String key = String.format("attempt_%s", user.get().getId());
        String count = redisValueUtility.getValue(key);

        if (count == null) {
          count = "1";
          redisValueUtility.setValue(key, count, 5, TimeUnit.MINUTES);
        } else {
          Integer newCount = Integer.parseInt(count) + 1;
          if (newCount > MAX_LOGIN_ATTEMPT_LIMIT) {
            return new BaseResponse<>(4290, "Too many login attempts", null);
          }
          redisValueUtility.setValue(key, String.valueOf(newCount), 5, TimeUnit.MINUTES);
        }

        return new BaseResponse<>(4001, "Invalid password!", null);
      }

      String accessToken = jwtUtility.generateToken(user.get());
      return new BaseResponse<>(2001, "Signed in successfully", accessToken);

    } catch (Exception exception) {
      exception.printStackTrace();
      throw exception;
    }
  }

  @Override
  public BaseResponse<String> createUserFromSignUp(SignUpRequest request) {
    try {
      if (request == null) {
        return new BaseResponse<>(4000, "Empty request", null);
      }

      if (ValidateValue.isEmpty(request.getUsername())) {
        return new BaseResponse<>(4000, "Username is required", null);
      }

      if (ValidateValue.isEmpty(request.getPassword())) {
        return new BaseResponse<>(4000, "Password is required", null);
      }

      if (ValidateValue.isEmpty(request.getFirstname())) {
        return new BaseResponse<>(4000, "Firstname is required", null);
      }

      if (ValidateValue.isEmpty(request.getEmail())) {
        return new BaseResponse<>(4000, "Email is required", null);
      }

      if (!PatternMatch.isEmailValid(request.getEmail())) {
        return new BaseResponse<>(4000, "Email is not valid", null);
      }

      Optional<User> existedUser = userRepository.findByUsernameOrEmail(request.getUsername(), request.getEmail());

      if (existedUser.isPresent()) {
        return new BaseResponse<>(4000, "User with username or email already exists", null);
      }

      Date createdDate = new Date();
      User newUser = new User();

      newUser.setId(UUID.randomUUID().toString());
      newUser.setUsername(request.getUsername());
      newUser.setEmail(request.getEmail());
      newUser.setAccountNonExpired(true);
      newUser.setAccountNonLocked(true);
      newUser.setEnabled(true);
      newUser.setCredentialsNonExpired(true);
      newUser.setIsUsing2FA(false);
      newUser.setCreatedDate(createdDate);
      newUser.setPassword(passwordEncoder.encode(request.getPassword()));
      newUser.setSecret(googleAuthenticatorService.generateSecretKey());

      Customer customer = new Customer();
      customer.setId(UUID.randomUUID().toString());
      customer.setFirstName(request.getFirstname());
      customer.setLastName(request.getLastname());
      customer.setCreatedDate(createdDate);
      customer.setCreatedBy("system");
      customer.setUpdatedDate(createdDate);
      customer.setUpdatedBy("system");

      UserMapping userMapping = new UserMapping();
      userMapping.setId(UUID.randomUUID().toString());
      userMapping.setUser(newUser);
      userMapping.setAuthor(null);
      userMapping.setCustomer(customer);
      userMapping.setIsActive(true);

      // Save user
      Boolean completed = userRepository.createCustomerUser(newUser, customer, userMapping);
      if (!completed) {
        return new BaseResponse<>(4000, "Failed to create user", null);
      }

      return new BaseResponse<>(2001, "Signed up successfully", null);

    } catch (Exception exception) {
      exception.printStackTrace();
      throw exception;
    }
  }
}
