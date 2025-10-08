package oww.banking.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import oww.banking.mapper.AccountMapper;
import oww.banking.mapper.UserMapper;
import oww.banking.util.AESUtil;
import oww.banking.util.CryptoUtil;
import oww.banking.vo.AccountVO;
import oww.banking.vo.UserVO;

@Slf4j
@Service
@Transactional
public class AccountService {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;

    @Autowired
    private AESUtil aesUtil;

    private Map<String, String> emailVerificationCodes = new HashMap<>();

    // 이메일 마스킹 유틸리티 메서드
    private String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***";
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }

    /**
     * 계좌 생성 (해시 기반)
     */
    public String createAccount(String name, String email, String password, String emailCode) {
        try {
            if (!verifyEmailCode(email, emailCode)) {
                return "이메일 인증번호가 올바르지 않습니다.";
            }

            String emailHash = CryptoUtil.generateEmailHash(email);

            if (accountMapper.existsByEmailHash(emailHash)) {
                return "이미 계좌가 존재하는 이메일입니다.";
            }

            UserVO user = userMapper.findByEmailHash(emailHash);
            if (user == null) {
                user = new UserVO();
                user.setUserEmail(email); // 실제 이메일 저장 (암호화 권장)
                user.setName(name);
                user.setActive(true);
                user.setRole("USER");
                user.setProvider("LOCAL");
                userMapper.createUser(user);
                user = userMapper.findByEmailHash(emailHash);
                if (user == null) return "사용자 생성에 실패했습니다.";
            }

            String accountNumber = generateBankStyleAccountNumber();

            AccountVO account = new AccountVO();
            account.setUserEmail(email); // 실제 이메일
            account.setAccountNumber(aesUtil.encrypt(accountNumber));
            account.setBalance(BigDecimal.ZERO);
            account.setAccountPassword(passwordEncoder.encode(password));

            int result = accountMapper.createAccount(account);

            if (result > 0) {
                emailVerificationCodes.remove(email);
                log.info("계좌 생성 성공: {}", maskEmail(email));
                return "계좌가 성공적으로 생성되었습니다.";
            } else {
                return "계좌 생성에 실패했습니다.";
            }

        } catch (Exception e) {
            log.error("계좌 생성 오류: {}", e.getMessage(), e);
            return "계좌 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private String generateBankStyleAccountNumber() {
        int sequenceValue = accountMapper.getNextAccountSequence();
        String bankCode = "1001";
        String branchCode = "2025";
        String serialNumber = String.format("%03d", sequenceValue);
        return bankCode + "-" + branchCode + "-" + serialNumber;
    }

    public String sendEmailVerification(String email) {
        String code = String.valueOf(100000 + new Random().nextInt(900000));
        emailVerificationCodes.put(email, code);

        // ✅ 실제 메일 발송
        emailService.sendVerificationEmail(email, code, null);

        log.info("인증번호 발송: {}", maskEmail(email));
        return code;
    }

    public boolean verifyEmailCode(String email, String inputCode) {
        String storedCode = emailVerificationCodes.get(email);
        boolean isValid = storedCode != null && storedCode.equals(inputCode);
        log.info("인증번호 확인: {} - {}", maskEmail(email), isValid ? "성공" : "실패");
        return isValid;
    }

    // ===============================
    // 해시 기반 조회 메서드들 (권장)
    // ===============================
    public AccountVO getAccountByEmailHash(String emailHash) {
        AccountVO account = accountMapper.findAccountByEmailHash(emailHash);
        if (account != null && account.getAccountNumber() != null) {
            try {
                // 복호화 시도
                account.setAccountNumber(aesUtil.decrypt(account.getAccountNumber()));
            } catch (Exception e) {
                // 복호화 실패 시 원본 사용 (평문인 경우)
                log.warn("계좌번호 복호화 실패, 원본 사용: {}", e.getMessage());
                // account.getAccountNumber() 그대로 사용
            }
        }
        return account;
    }

    public boolean existsByEmailHash(String emailHash) {
        return accountMapper.existsByEmailHash(emailHash);
    }

    // ===============================
    // 이메일 기반 메서드들 (호환성용, 내부에서 해시로 변환)
    // ===============================
    public AccountVO getAccountByEmail(String email) {
        String emailHash = CryptoUtil.generateEmailHash(email);
        log.info("계좌 조회: {}", maskEmail(email));
        return getAccountByEmailHash(emailHash);
    }

    public boolean isAccountExists(String userEmail) {
        String emailHash = CryptoUtil.generateEmailHash(userEmail);
        return existsByEmailHash(emailHash);
    }

    // ===============================
    // 기타 계좌 관련 메서드들
    // ===============================
    public boolean verifyAccountPassword(int accountId, String password) {
        return accountMapper.verifyPassword(accountId, password);
    }

    public boolean updateBalance(int accountId, BigDecimal newBalance) {
        return accountMapper.updateBalance(accountId, newBalance) > 0;
    }
}