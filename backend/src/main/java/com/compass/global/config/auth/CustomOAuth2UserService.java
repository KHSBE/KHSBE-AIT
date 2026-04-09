package com.compass.global.config.auth;

import com.compass.domain.user.dao.UserRepository;
import com.compass.domain.user.domain.Role;
import com.compass.domain.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Value("${app.admin.emails:}")
    private String adminEmails;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        var delegate = new DefaultOAuth2UserService();
        var oAuth2User = delegate.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        Role resolvedRole = resolveRole(email);

        User user = userRepository.findByEmail(email)
                .map(existing -> {
                    existing.updateProfile(name, resolvedRole);
                    return existing;
                })
                .orElseGet(() -> User.builder()
                        .email(email)
                        .nickname(name)
                        .role(resolvedRole)
                        .build());

        userRepository.save(user);
        return oAuth2User;
    }

    private Role resolveRole(String email) {
        if (email == null || email.isBlank()) {
            return Role.USER;
        }

        Set<String> admins = Arrays.stream(adminEmails.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        return admins.contains(email.trim().toLowerCase()) ? Role.ADMIN : Role.USER;
    }
}
