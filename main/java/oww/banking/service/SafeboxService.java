package oww.banking.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import oww.banking.mapper.AccountMapper;
import oww.banking.mapper.SafeboxMapper;
import oww.banking.util.CryptoUtil;
import oww.banking.vo.AccountVO;
import oww.banking.vo.SafeboxGoalVO;
import oww.banking.vo.SafeboxHistoryVO;
import oww.banking.vo.SafeboxVO;

@Slf4j
@Service
public class SafeboxService {

    @Autowired
    private SafeboxMapper safeboxMapper;

    @Autowired
    private AccountMapper accountMapper;

    /**
     * 세이프박스 금액 설정 (해시 기반)
     */
    @Transactional
    public boolean setSafeboxAmount(String emailHash, BigDecimal amount) {
        try {
            // 계좌 조회 (해시로 직접 조회)
            AccountVO account = accountMapper.findAccountByEmailHash(emailHash);
            if (account == null) {
                log.error("계좌를 찾을 수 없습니다: emailHash={}", emailHash);
                throw new RuntimeException("계좌 없음");
            }

            // 세이프박스 조회 (해시로 직접 조회)
            SafeboxVO safebox = safeboxMapper.findSafeboxByEmailHash(emailHash);
            if (safebox == null) {
                // 새 세이프박스 생성 시 userEmail과 emailHash 모두 설정
                SafeboxVO newBox = new SafeboxVO();
                newBox.setUserEmail(account.getUserEmail()); // 계좌에서 실제 이메일 가져오기
                newBox.setEmailHash(emailHash);
                newBox.setBalance(amount);
                safeboxMapper.createSafebox(newBox);
            } else {
                BigDecimal newBalance = safebox.getBalance().add(amount);
                Map<String, Object> params = new HashMap<>();
                params.put("safeboxId", safebox.getSafeboxId());
                params.put("balance", newBalance);
                safeboxMapper.updateSafeboxBalance(params);
            }

            // 계좌 잔액 차감
            BigDecimal newAccountBalance = account.getBalance().subtract(amount);
            if (newAccountBalance.compareTo(BigDecimal.ZERO) < 0) {
                log.error("잔액 부족: 요청금액={}, 계좌잔액={}", amount, account.getBalance());
                throw new RuntimeException("잔액 부족");
            }

            account.setBalance(newAccountBalance);
            accountMapper.updateBalance(account.getAccountId(), newAccountBalance);

            log.info("세이프박스 금액 설정 성공: amount={}", amount);
            return true;
        } catch (Exception e) {
            log.error("세이프박스 금액 설정 실패: ", e);
            return false;
        }
    }

    /**
     * 세이프박스 조회 (해시 기반)
     */
    public SafeboxVO getSafeboxByEmailHash(String emailHash) {
        return safeboxMapper.findSafeboxByEmailHash(emailHash);
    }

    /**
     * 정기저금 목표 조회 (해시 기반)
     */
    public List<SafeboxGoalVO> getSavingGoals(String emailHash) {
        SafeboxVO safebox = getSafeboxByEmailHash(emailHash);
        if (safebox == null) return null;
        return safeboxMapper.findGoalsBySafeboxId(safebox.getSafeboxId());
    }

    /**
     * 정기저금 목표 생성 (해시 기반)
     */
    @Transactional
    public boolean createSavingGoal(String emailHash, String title, BigDecimal targetAmount,
                                    LocalDate startDate, LocalDate endDate, String paymentType) {
        try {
            SafeboxVO safebox = safeboxMapper.findSafeboxByEmailHash(emailHash);
            if (safebox == null) {
                log.error("세이프박스가 존재하지 않습니다: emailHash={}", emailHash);
                throw new RuntimeException("세이프박스가 존재하지 않습니다.");
            }

            SafeboxGoalVO goal = new SafeboxGoalVO(
                safebox.getSafeboxId(), // safeboxId
                title,
                targetAmount,
                startDate,
                endDate,
                paymentType
            );

            safeboxMapper.createSafeboxGoal(goal);
            log.info("정기저금 목표 생성 성공: title={}, targetAmount={}", title, targetAmount);
            return true;
        } catch (Exception e) {
            log.error("정기저금 목표 생성 실패: ", e);
            return false;
        }
    }

    /**
     * 저축 내역 조회
     */
    public List<SafeboxHistoryVO> getSavingHistory(int goalId) {
        return safeboxMapper.findHistoryByGoalId(goalId);
    }

    /**
     * 목표별 총 저축액
     */
    public BigDecimal getTotalSavedAmount(int goalId) {
        BigDecimal result = safeboxMapper.getTotalSavedAmountByGoalId(goalId);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * 세이프박스 존재 여부 (해시 기반)
     */
    public boolean hasSafebox(String emailHash) {
        return safeboxMapper.existsByEmailHash(emailHash);
    }

    /**
     * 사용자 전체 자산 조회 (해시 기반)
     */
    public BigDecimal getTotalAssets(String emailHash) {
        AccountVO account = accountMapper.findAccountByEmailHash(emailHash);
        SafeboxVO safebox = safeboxMapper.findSafeboxByEmailHash(emailHash);

        BigDecimal accountBalance = account != null ? account.getBalance() : BigDecimal.ZERO;
        BigDecimal safeboxBalance = safebox != null ? safebox.getBalance() : BigDecimal.ZERO;

        return accountBalance.add(safeboxBalance);
    }

    /**
     * 사용자 전체 저축 내역 조회 (해시 기반)
     */
    public List<SafeboxHistoryVO> getFullSavingHistory(String emailHash) {
        return safeboxMapper.findHistoryByUserEmailHash(emailHash);
    }

    // ===============================
    // 호환성용 메서드들 (이메일을 받아서 내부에서 해시로 변환)
    // ===============================
    
    /**
     * 세이프박스 조회 (이메일 기반 - 호환성용)
     */
    @Deprecated
    public SafeboxVO getSafeboxByEmail(String email) {
        String emailHash = CryptoUtil.generateEmailHash(email);
        return getSafeboxByEmailHash(emailHash);
    }

    /**
     * 세이프박스 금액 설정 (이메일 기반 - 호환성용)
     */
    @Deprecated
    public boolean setSafeboxAmountByEmail(String email, BigDecimal amount) {
        String emailHash = CryptoUtil.generateEmailHash(email);
        return setSafeboxAmount(emailHash, amount);
    }

    /**
     * 정기저금 목표 조회 (이메일 기반 - 호환성용)
     */
    @Deprecated
    public List<SafeboxGoalVO> getSavingGoalsByEmail(String email) {
        String emailHash = CryptoUtil.generateEmailHash(email);
        return getSavingGoals(emailHash);
    }

    /**
     * 정기저금 목표 생성 (이메일 기반 - 호환성용)
     */
    @Deprecated
    public boolean createSavingGoalByEmail(String email, String title, BigDecimal targetAmount,
                                           LocalDate startDate, LocalDate endDate, String paymentType) {
        String emailHash = CryptoUtil.generateEmailHash(email);
        return createSavingGoal(emailHash, title, targetAmount, startDate, endDate, paymentType);
    }
}