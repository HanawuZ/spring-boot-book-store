package com.backend.app.userservice.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SignInRequest {
     @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;
}
