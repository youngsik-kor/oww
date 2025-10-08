package com.oww.login.service;

import com.oww.login.entity.User;
import com.oww.login.oauth.CustomOAuth2User;
import com.oww.login.repository.UserRepository;
import com.oww.login.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    // 이메일 마스킹 유틸리티 메서드
    private String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***";
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes);

        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // 이메일해시로 사용자 검색 
        String emailHash = CryptoUtil.generateEmailHash(userInfo.getEmail());
        Optional<User> userOptional = userRepository.findByUserEmailHash(emailHash);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            // 기존 사용자인 경우 정보 업데이트
            user = updateExistingUser(user, userInfo);
            
        } else {
            // 새로운 사용자 생성
            user = createNewUser(userInfo);
        }

        return new CustomOAuth2User(user, attributes, userNameAttributeName); 
    }

    private User createNewUser(OAuth2UserInfo userInfo) {
        User user = User.createSocialUser(
                userInfo.getEmail(),
                userInfo.getName(),
                userInfo.getProvider(),
                userInfo.getId()
        );

        // 로그에는 마스킹된 이메일만 출력
        log.info("새 소셜 사용자 생성: {} ({})", 
                maskEmail(userInfo.getEmail()), userInfo.getProvider());
        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo userInfo) {
        // 기존 사용자는 이름만 업데이트
        existingUser.setName(userInfo.getName());
        
        // 로그에는 마스킹된 이메일만 출력
        log.info("기존 사용자 정보 업데이트: {} ({})", 
                maskEmail(userInfo.getEmail()), userInfo.getProvider());
        return userRepository.save(existingUser);
    }
}

abstract class OAuth2UserInfo {
    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public abstract String getId();
    public abstract String getName();
    public abstract String getEmail();
    public abstract String getImageUrl();
    public abstract User.Provider getProvider();

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}

class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if ("github".equals(registrationId)) {
            return new GitHubOAuth2UserInfo(attributes);
        } else {
            throw new IllegalArgumentException("Sorry! Login with " + registrationId + " is not supported yet.");
        }
    }
}

class GoogleOAuth2UserInfo extends OAuth2UserInfo {
    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("picture");
    }

    @Override
    public User.Provider getProvider() {
        return User.Provider.GOOGLE;
    }
}

class GitHubOAuth2UserInfo extends OAuth2UserInfo {
    public GitHubOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getName() {
        String name = (String) attributes.get("name");
        return name != null ? name : (String) attributes.get("login");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("avatar_url");
    }

    @Override
    public User.Provider getProvider() {
        return User.Provider.GITHUB;
    }
}