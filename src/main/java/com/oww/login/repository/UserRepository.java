package com.oww.login.repository;

import com.oww.login.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> { // PK가 String(USER_EMAIL)

    // 해시로 검색 (보안 강화) - 메서드명 통일
    @Query("SELECT u FROM User u WHERE u.userEmailHash = :emailHash")
    Optional<User> findByUserEmailHash(@Param("emailHash") String emailHash);

    // Provider로 찾기
    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId")
    Optional<User> findByProviderAndProviderId(@Param("provider") User.Provider provider, @Param("providerId") String providerId);

    // 해시로 존재 여부 확인 - 메서드명 통일
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.userEmailHash = :emailHash")
    boolean existsByUserEmailHash(@Param("emailHash") String emailHash);

    // 활성 사용자 해시 검색 - 메서드명 통일
    @Query("SELECT u FROM User u WHERE u.userEmailHash = :emailHash AND u.isActive = true")
    Optional<User> findByUserEmailHashAndIsActiveTrue(@Param("emailHash") String emailHash);

    // 기존 이메일 직접 검색 (내부용, 가급적 사용 금지)
    @Deprecated
    @Query("SELECT u FROM User u WHERE u.userEmail = :email")
    Optional<User> findByUserEmail(@Param("email") String email);
}