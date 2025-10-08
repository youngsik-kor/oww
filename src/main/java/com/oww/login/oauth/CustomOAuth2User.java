package com.oww.login.oauth;

import com.oww.login.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {

    private User user;
    private Map<String, Object> attributes;
    private String nameAttributeKey;

    // 기존 생성자 (호환성 유지)
    public CustomOAuth2User(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
        this.nameAttributeKey = "name"; // 기본값
    }

    // userNameAttributeName을 포함한 새 생성자
    public CustomOAuth2User(User user, Map<String, Object> attributes, String nameAttributeKey) {
        this.user = user;
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    @Override
    public String getName() {
        // nameAttributeKey에 따라 적절한 값 반환
        if (nameAttributeKey != null && attributes.containsKey(nameAttributeKey)) {
            return String.valueOf(attributes.get(nameAttributeKey));
        }
        // fallback으로 User 엔티티의 name 사용
        return user.getName();
    }

    // 실제 이메일 반환 (내부 처리용)
    public String getEmail() {
        return user.getUserEmail();
    }

    // 해시된 이메일 반환 (보안용)
    public String getEmailHash() {
        return user.getUserEmailHash();
    }

    // Entity 필드명에 맞춰 수정
    public Long getId() {
        return user.getUserno();
    }

    public User.Provider getProvider() {
        return user.getProvider();
    }

    // 마스킹된 이메일 반환 (로깅용)
    public String getMaskedEmail() {
        String email = user.getUserEmail();
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***";
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }
}