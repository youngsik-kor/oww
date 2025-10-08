package com.oww.login.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.oww.login.util.CryptoUtil;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    // PK는 USER_EMAIL 유지
    @Id
    @Column(name = "USER_EMAIL", nullable = false, length = 100)
    private String userEmail;

    // USERNO는 일반 컬럼 (GeneratedValue 제거 - PK가 아니므로)
    @Column(name = "USERNO", precision = 19)
    private Long userno; // 변수명을 소문자로 통일

    @Column(name = "USER_EMAIL_HASH", length = 64)
    private String userEmailHash;

    @Column(name = "NAME", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 50)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "PROVIDER", nullable = false, length = 255)
    private Provider provider;

    @Column(name = "PROVIDER_ID", length = 255)
    private String providerId;

    @Column(name = "IS_ACTIVE", nullable = false, precision = 1)
    @Convert(converter = BooleanToNumberConverter.class)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    public enum Role {
        USER, ADMIN
    }

    public enum Provider {
        GOOGLE, GITHUB, LOCAL
    }

    // 커스텀 setter (해시 자동 생성)
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            this.userEmailHash = CryptoUtil.generateEmailHash(userEmail);
        }
    }

    // 소셜 로그인용 생성자
    public static User createSocialUser(String email, String name, Provider provider, String providerId) {
        User user = User.builder()
                .name(name)
                .provider(provider)
                .providerId(providerId)
                .role(Role.USER)
                .isActive(true)
                .build();
        user.setUserEmail(email);
        return user;
    }

    // 일반 로그인용 생성자
    public static User createLocalUser(String email, String name) {
        User user = User.builder()
                .name(name)
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .isActive(true)
                .build();
        user.setUserEmail(email);
        return user;
    }

    @Converter
    public static class BooleanToNumberConverter implements AttributeConverter<Boolean, Integer> {
        @Override
        public Integer convertToDatabaseColumn(Boolean attribute) {
            return attribute != null && attribute ? 1 : 0;
        }

        @Override
        public Boolean convertToEntityAttribute(Integer dbData) {
            return dbData != null && dbData == 1;
        }
    }
}