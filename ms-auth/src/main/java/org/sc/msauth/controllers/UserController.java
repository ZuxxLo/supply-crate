package org.sc.msauth.controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.sc.commonconfig.ApiResponse;
import org.sc.msauth.dto.LoginDTO;
import org.sc.msauth.dto.RegisterDTO;
import org.sc.msauth.dto.UserDTO;
import org.sc.msauth.services.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDTO>> createUser(@Valid @RequestBody RegisterDTO registerDTO) {
        try {
            UserDTO createdUser = authService.createUser(registerDTO);
            ApiResponse<UserDTO> response = new ApiResponse<>(createdUser, "User created successfully");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            ApiResponse<UserDTO> response = new ApiResponse<>(null, "Error creating user: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        ApiResponse<String> response = new ApiResponse<>("Email verified successfully", "Success");
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        List<UserDTO> users = authService.getAllUsers();
        ApiResponse<List<UserDTO>> response = new ApiResponse<>(users, "Users fetched successfully");
        return ResponseEntity.ok(response);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
        UserDTO userDTO = authService.getUserById(id);
        if (userDTO == null) {
            ApiResponse<UserDTO> response = new ApiResponse<>(null, "User not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        ApiResponse<UserDTO> response = new ApiResponse<>(userDTO, "User details fetched successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = authService.updateUser(id, userDTO);
        ApiResponse<UserDTO> response = new ApiResponse<>(updatedUser, "User updated successfully");
        return ResponseEntity.ok(response);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        ApiResponse<Void> response = new ApiResponse<>(null, "User deleted successfully");
        return ResponseEntity.ok(response);
    }
//http://localhost:7777/service-auth/oauth2/authorization/google
@GetMapping(value = "/oauth2/success", produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE })
public ResponseEntity<?> oauth2Success(
        @AuthenticationPrincipal OAuth2User oauthUser,
        @RequestHeader(value = "Accept", defaultValue = MediaType.APPLICATION_JSON_VALUE) String acceptHeader) {

    String email = oauthUser.getAttribute("email");
    String name = oauthUser.getAttribute("name");

    UserDTO dto = authService.processOAuthPostLogin(email, name);
    ApiResponse<UserDTO> response = new ApiResponse<>(dto, "OAuth2 login successful");
    System.out.println(response);
    String userJson = "";
    try {
        // Convert response to JSON string
        userJson = new ObjectMapper().writeValueAsString(response);
    } catch (JsonProcessingException e) {
        e.printStackTrace();
        // handle error or return some fallback JSON
        userJson = "{\"message\":\"error\"}";
    }

    if (acceptHeader.contains(MediaType.TEXT_HTML_VALUE)) {
        // Return HTML page with JS to send data via postMessage and close popup
        String html = "<!DOCTYPE html><html><head><title>Login Success</title></head><body>" +
                "<script>" +
                "const user = " + userJson + ";" +
                "if(window.opener){" +
                "window.opener.postMessage({type:'oauth-success', user}, 'http://localhost:4200');" +
                "window.close();" +
                "}" +
                "</script>" +
                "<p>Login successful, closing...</p>" +
                "</body></html>";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        return new ResponseEntity<>(html, headers, HttpStatus.OK);
    } else {
        // Return JSON response for API clients/mobile
        return ResponseEntity.ok(response);
    }
}

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDTO>> login(@RequestBody LoginDTO request) {
        Optional<UserDTO> userOpt = authService.login(request.getEmail(), request.getPassword());

        if (userOpt.isEmpty()) {
            ApiResponse<UserDTO> response = new ApiResponse<>(null, "Invalid email or password");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        ApiResponse<UserDTO> response = new ApiResponse<>(userOpt.get(), "Login successful");
        return ResponseEntity.ok(response);
    }

}