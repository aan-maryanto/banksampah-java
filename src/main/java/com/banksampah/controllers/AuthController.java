package com.banksampah.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banksampah.entity.Role;
import com.banksampah.entity.RoleType;
import com.banksampah.entity.User;
import com.banksampah.payload.request.LoginRequest;
import com.banksampah.payload.request.RegisterRequest;
import com.banksampah.payload.response.JwtResponse;
import com.banksampah.payload.response.MessageResponse;
import com.banksampah.repository.RoleRepository;
import com.banksampah.repository.UserRepository;
import com.banksampah.security.jwt.JwtUtils;
import com.banksampah.services.UserDetailsImp;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate( new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImp userDetailsImp = (UserDetailsImp) authentication.getPrincipal();
        List<String> roles = userDetailsImp.getAuthorities().stream().map(role -> role.getAuthority()).collect(Collectors.toList());
        return ResponseEntity.ok(new JwtResponse(jwt, "Bearer", userDetailsImp.getId(), userDetailsImp.getUsername(), userDetailsImp.getEmail(), roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if(userRepository.existByUsername(registerRequest.getUsername())) {
            return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Error: Username is already exists!"));
        }

        if(userRepository.existByEmail(registerRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already exists!"));
        }

        User user = new User(registerRequest.getUsername(), registerRequest.getEmail(), passwordEncoder.encode(registerRequest.getPassword()));
        Set<String> strRole = registerRequest.getRoles();
        Set<Role> roles = new HashSet<>();
        if(strRole == null){
            Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error : Role is not found"));
            roles.add(userRole);
        }else{
            strRole.forEach(role -> {
				switch (role) {
				case "admin":
					Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(adminRole);
					break;
				case "partner":
					Role modRole = roleRepository.findByName(RoleType.ROLE_PARTNER)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(modRole);
					break;
				default:
					Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(userRole);
				}
			});
        }
        user.setRoles(roles);
		userRepository.save(user);
		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    } 
}
