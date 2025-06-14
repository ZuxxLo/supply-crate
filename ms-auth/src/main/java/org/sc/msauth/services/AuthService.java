package org.sc.msauth.services;


import lombok.RequiredArgsConstructor;
import org.sc.commonconfig.JwtUtil;

import org.sc.msauth.dto.RegisterDTO;
import org.sc.msauth.dto.UserDTO;
import org.sc.msauth.entities.Role;
import org.sc.msauth.entities.User;
import org.sc.msauth.entities.VerificationToken;
import org.sc.msauth.repositories.RoleRepository;
import org.sc.msauth.repositories.UserRepository;
import org.sc.msauth.repositories.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.mail.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    @Autowired
    private final UserRepository userRepository;
    //  private final RoleRepository roleRepository;
    private final VerificationTokenRepository tokenRepository;

    @Autowired
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private final EmailService emailService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private final JwtUtil jwtUtil;

    @Autowired
    private final RoleRepository roleRepository;

    private Role getRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + name));
    }

    @Transactional
    public UserDTO createUser(RegisterDTO userDTO) throws MessagingException {
        if (userDTO.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        if (!userDTO.getPassword().equals(userDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password must match");
        }

        if (userRepository.findByUsername(userDTO.getUsername()).isPresent() ||
                userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Username or email already exists");
        }

        User user = User.builder()
                .username(userDTO.getUsername())
                .email(userDTO.getEmail())
                .password(passwordEncoder.encode(userDTO.getPassword()))
                .firstName(userDTO.getFirstName())
                .lastName(userDTO.getLastName())
                .phone(userDTO.getPhone())
                .address(userDTO.getAddress())
                .roles(Set.of(getRoleByName("USER")))
                .emailVerified(false)
                .build();
        int serverPort = 0;
        user = userRepository.save(user);
        if (context instanceof ServletWebServerApplicationContext) {
            ServletWebServerApplicationContext webServerAppContext =
                    (ServletWebServerApplicationContext) context;
            serverPort = webServerAppContext.getWebServer().getPort();
        }
        String verificationUrl = "http://localhost:7777/service-auth/api/users/verify-email";

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();

        tokenRepository.save(verificationToken);
        emailService.sendVerificationEmail(user.getEmail(), token, verificationUrl);

        return mapToDTO(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (verificationToken.isExpired()) {
            throw new IllegalArgumentException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return null;
        }
        return mapToDTO(user);
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }
        user.setFirstName(user.getFirstName());
        user.setLastName(user.getLastName());
        user.setPhone(userDTO.getPhone());
        user.setAddress(userDTO.getAddress());
        // user.setRoles(getRolesFromNames(userDTO.getRoleNames()));
        user.setEmailVerified(userDTO.isEmailVerified());

        user = userRepository.save(user);
        return mapToDTO(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found");
        }
        userRepository.deleteById(id);
    }

//    private Set<Role> getRolesFromNames(Set<String> roleNames) {
//        if (roleNames == null) return new HashSet<>();
//        return roleNames.stream()
//                .map(name -> roleRepository.findByName(name)
//                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + name)))
//                .collect(Collectors.toSet());
//    }

    private UserDTO mapToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setRoleNames(user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()));
        dto.setEmailVerified(user.isEmailVerified());
        return dto;
    }


    @Transactional
    public UserDTO processOAuthPostLogin(String email, String name) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        User user;
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
        } else {
            String firstName = name;
            String lastName = "";

            if (name != null && name.contains(" ")) {
                String[] parts = name.trim().split("\\s+");
                firstName = parts[0];
                lastName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
            }

            String baseUsername = email.split("@")[0];
            String username = baseUsername;
            int counter = 1;

            while (userRepository.findByUsername(username).isPresent()) {
                username = baseUsername + counter;
                counter++;
            }

            user = User.builder()
                    .username(username)
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .emailVerified(true)
                    .password("")
                    .roles(Set.of(getRoleByName("USER")))
                    .build();

            userRepository.save(user);
        }

        UserDTO userDTO = mapToDTO(user);
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), roleNames);
        userDTO.setToken(token);

        return userDTO;
    }


    public Optional<UserDTO> login(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .map(user -> {

                    List<String> roleNames = user.getRoles().stream()
                            .map(Role::getName)
                            .collect(Collectors.toList());

                    UserDTO dto = mapToDTO(user);


                    String token = jwtUtil.generateToken(user.getId(), user.getEmail(), roleNames);
                    dto.setToken(token);
                    return dto;
                });
    }


}