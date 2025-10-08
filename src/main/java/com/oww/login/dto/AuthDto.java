package com.oww.login.dto;

import com.oww.login.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long userno;
        private String userEmailHash;
        private String name;
        private User.Role role;
        private User.Provider provider;
        private Boolean isActive;

        public static UserInfo from(User user) {
            return UserInfo.builder()
                    .userno(user.getUserno()) // getUserNo() → getUserno()로 수정
                    .userEmailHash(user.getUserEmailHash())
                    .name(user.getName())
                    .role(user.getRole())
                    .provider(user.getProvider())
                    .isActive(user.getIsActive())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialLoginResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private UserInfo userInfo;
        private boolean isNewUser;
    }
}