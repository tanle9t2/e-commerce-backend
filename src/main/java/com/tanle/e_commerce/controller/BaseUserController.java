package com.tanle.e_commerce.controller;

import com.tanle.e_commerce.payload.MessageResponse;
import com.tanle.e_commerce.request.LoginRequest;
import com.tanle.e_commerce.service.TokenSerice;
import com.tanle.e_commerce.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/")
public abstract class BaseUserController {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    protected TokenSerice tokenSerice;
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<MessageResponse> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(),request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            MessageResponse tokenMessage= tokenSerice.registerToken(request.getUsername());
            MessageResponse messageResponse = MessageResponse.builder()
                    .data(tokenMessage.getData())
                    .status(HttpStatus.OK)
                    .message("login successfully")
                    .build();
            return new ResponseEntity<>(messageResponse,HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
    @PostMapping("/logout")
    public MessageResponse logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null)
            return MessageResponse.builder()
                    .message("Unauthorized")
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        String username= authentication.getName();
        userService.updateLastAccess(username);
        tokenSerice.revokeToken(username);
        SecurityContextHolder.clearContext();

        return MessageResponse.builder()
                .status(HttpStatus.OK)
                .message("Logout successfully")
                .build();
    }
}
