package oww.banking.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import oww.banking.mapper.TransferMapper;
import oww.banking.util.AESUtil;
import oww.banking.util.CryptoUtil;
import oww.banking.vo.TransferVO;
import oww.banking.vo.TransferHistoryVO;

@Slf4j
@Service
public class TransferService {

    @Autowired
    private TransferMapper transferMapper;

    @Autowired
    private AESUtil aesUtil;

    /**
     * 이체 처리 (fromEmailHash + toAccountNumber)
     */
    @Transactional
    public String processTransferByEmailHash(String fromEmailHash, String toAccountNumber, BigDecimal amount, 
                                             String memo, String password) {
        try {
            // 1. 보내는 계좌 정보 조회 (해시 기반)
            Integer fromAccountId = transferMapper.findAccountIdByEmailHash(fromEmailHash);
            if (fromAccountId == null) {
                log.error("보내는 계좌가 존재하지 않습니다: fromEmailHash={}", fromEmailHash);
                return "보내는 계좌가 존재하지 않습니다.";
            }

            // 2. 계좌 비밀번호 확인 (BCrypt 사용)
            String hashedPassword = transferMapper.getAccountPassword(fromAccountId);
            if (hashedPassword == null) {
                log.warn("계좌 비밀번호를 찾을 수 없습니다: fromAccountId={}", fromAccountId);
                return "계좌 정보가 올바르지 않습니다.";
            }
            
            if (!BCrypt.checkpw(password, hashedPassword)) {
                log.warn("계좌 비밀번호 불일치: fromAccountId={}", fromAccountId);
                return "계좌 비밀번호가 일치하지 않습니다.";
            }

         // 3. 받는 계좌 정보 조회 (계좌번호 기반)
            String encryptedToAccountNumber = aesUtil.encrypt(toAccountNumber);
            Integer toAccountId = transferMapper.findAccountIdByNumber(encryptedToAccountNumber);
            if (toAccountId == null) {
                log.error("받는 계좌가 존재하지 않습니다: toAccountNumber={}", toAccountNumber);
                return "받는 계좌가 존재하지 않습니다.";
            }

            // 4. 자기 자신에게 이체 방지
            if (fromAccountId.equals(toAccountId)) {
                return "본인 계좌로는 이체할 수 없습니다.";
            }

            // 5. 잔액 확인
            BigDecimal fromBalance = transferMapper.getBalanceByAccountId(fromAccountId);
            if (fromBalance == null || fromBalance.compareTo(amount) < 0) {
                log.warn("잔액 부족: fromBalance={}, requestAmount={}", fromBalance, amount);
                return "잔액이 부족합니다.";
            }

            // 6. 이체 금액 유효성 검사
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return "이체 금액은 0원보다 커야 합니다.";
            }

            // 7. 잔액 업데이트
            BigDecimal newFromBalance = fromBalance.subtract(amount);
            BigDecimal toBalance = transferMapper.getBalanceByAccountId(toAccountId);
            BigDecimal newToBalance = (toBalance != null ? toBalance : BigDecimal.ZERO).add(amount);

            transferMapper.updateBalance(fromAccountId, newFromBalance);
            transferMapper.updateBalance(toAccountId, newToBalance);

            // 8. 이체 내역 저장
            TransferVO transfer = new TransferVO();
            transfer.setFromAccountId(fromAccountId);
            transfer.setToAccountId(toAccountId);
            transfer.setAmount(amount);
            transfer.setMemo(memo != null ? memo : "");
            transferMapper.insertTransfer(transfer);

            // 9. 거래내역 저장 (보내는 계좌)
            TransferHistoryVO fromHistory = new TransferHistoryVO();
            fromHistory.setAccountId(fromAccountId);
            fromHistory.setTxType("TRANSFER_OUT");
            fromHistory.setAmount(amount);
            fromHistory.setMemo(memo != null ? memo : "");
            transferMapper.insertTransferHistory(fromHistory);

            // 10. 거래내역 저장 (받는 계좌)
            TransferHistoryVO toHistory = new TransferHistoryVO();
            toHistory.setAccountId(toAccountId);
            toHistory.setTxType("TRANSFER_IN");
            toHistory.setAmount(amount);
            toHistory.setMemo(memo != null ? memo : "");
            transferMapper.insertTransferHistory(toHistory);

            log.info("이체 처리 성공: amount={}", amount);
            return "이체가 성공적으로 완료되었습니다.";

        } catch (Exception e) {
            log.error("이체 처리 중 오류 발생: ", e);
            throw new RuntimeException("이체 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    
    /**
     * 계좌번호로 계좌 정보 조회 (암호화 처리)
     */
    public Map<String, Object> getAccountInfoByNumber(String plainAccountNumber) {
        try {
            System.out.println("=== 계좌번호 조회 디버깅 시작 ===");
            System.out.println("입력받은 평문 계좌번호: [" + plainAccountNumber + "]");
            
            String formattedAccountNumber = formatAccountNumber(plainAccountNumber);
            System.out.println("DB 형식으로 포맷 후: [" + formattedAccountNumber + "]");
            
            String encryptedAccountNumber = aesUtil.encrypt(formattedAccountNumber);
            System.out.println("암호화된 계좌번호: [" + encryptedAccountNumber + "]");
            
            Map<String, Object> accountInfo = transferMapper.getAccountInfoByNumber(encryptedAccountNumber);
            System.out.println("조회 결과: " + (accountInfo != null ? "찾음" : "없음"));
            
            // 계좌번호 복호화해서 반환
            if (accountInfo != null && accountInfo.get("ACCOUNT_NUMBER") != null) {
                try {
                    String encryptedAccount = (String) accountInfo.get("ACCOUNT_NUMBER");
                    String decryptedAccount = aesUtil.decrypt(encryptedAccount);
                    accountInfo.put("ACCOUNT_NUMBER", decryptedAccount);
                    System.out.println("복호화된 계좌번호로 교체: " + decryptedAccount);
                } catch (Exception e) {
                    System.out.println("계좌번호 복호화 실패: " + e.getMessage());
                }
            }
            
            return accountInfo;
            
        } catch (Exception e) {
            System.out.println("오류 발생: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private String formatAccountNumber(String accountNumber) {
        // 1. 입력값에서 모든 하이픈 제거
        String cleaned = accountNumber.replaceAll("-", "");
        
        // 2. 11자리인 경우 DB 형식(1001-2025-063)으로 포맷팅
        if (cleaned.length() == 11) {
            return cleaned.substring(0, 4) + "-" + 
                   cleaned.substring(4, 8) + "-" + 
                   cleaned.substring(8);
        }
        
        // 3. 길이가 맞지 않으면 원본 반환
        return accountNumber;
    }
    
    

    /**
     * 해시 간 이체 처리 (두 해시값 모두 사용)
     */
    @Transactional
    public String processTransferByEmailHashes(String fromEmailHash, String toEmailHash, BigDecimal amount, 
                                               String memo, String password) {
        try {
            // 1. 보내는 계좌 정보 조회
            Integer fromAccountId = transferMapper.findAccountIdByEmailHash(fromEmailHash);
            if (fromAccountId == null) return "보내는 계좌가 존재하지 않습니다.";

            // 2. 계좌 비밀번호 확인 (BCrypt 사용)
            String hashedPassword = transferMapper.getAccountPassword(fromAccountId);
            if (hashedPassword == null) {
                log.warn("계좌 비밀번호를 찾을 수 없습니다: fromAccountId={}", fromAccountId);
                return "계좌 정보가 올바르지 않습니다.";
            }
            
            if (!BCrypt.checkpw(password, hashedPassword)) {
                log.warn("계좌 비밀번호 불일치: fromAccountId={}", fromAccountId);
                return "계좌 비밀번호가 일치하지 않습니다.";
            }

            // 3. 받는 계좌 정보 조회
            Integer toAccountId = transferMapper.findAccountIdByEmailHash(toEmailHash);
            if (toAccountId == null) return "받는 계좌가 존재하지 않습니다.";

            // 4. 자기 자신에게 이체 방지
            if (fromAccountId.equals(toAccountId)) {
                return "본인 계좌로는 이체할 수 없습니다.";
            }

            // 5. 잔액 확인
            BigDecimal fromBalance = transferMapper.getBalanceByAccountId(fromAccountId);
            if (fromBalance == null || fromBalance.compareTo(amount) < 0) return "잔액이 부족합니다.";

            // 6. 이체 금액 유효성 검사
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return "이체 금액은 0원보다 커야 합니다.";

            // 7. 잔액 업데이트
            BigDecimal newFromBalance = fromBalance.subtract(amount);
            BigDecimal toBalance = transferMapper.getBalanceByAccountId(toAccountId);
            BigDecimal newToBalance = (toBalance != null ? toBalance : BigDecimal.ZERO).add(amount);

            transferMapper.updateBalance(fromAccountId, newFromBalance);
            transferMapper.updateBalance(toAccountId, newToBalance);

            // 8. 이체 내역 저장
            TransferVO transfer = new TransferVO();
            transfer.setFromAccountId(fromAccountId);
            transfer.setToAccountId(toAccountId);
            transfer.setAmount(amount);
            transfer.setMemo(memo != null ? memo : "");
            transferMapper.insertTransfer(transfer);

            // 9. 거래내역 저장 (보내는 계좌)
            TransferHistoryVO fromHistory = new TransferHistoryVO();
            fromHistory.setAccountId(fromAccountId);
            fromHistory.setTxType("TRANSFER_OUT");
            fromHistory.setAmount(amount);
            fromHistory.setMemo(memo != null ? memo : "");
            transferMapper.insertTransferHistory(fromHistory);

            // 10. 거래내역 저장 (받는 계좌)
            TransferHistoryVO toHistory = new TransferHistoryVO();
            toHistory.setAccountId(toAccountId);
            toHistory.setTxType("TRANSFER_IN");
            toHistory.setAmount(amount);
            toHistory.setMemo(memo != null ? memo : "");
            transferMapper.insertTransferHistory(toHistory);

            return "이체가 성공적으로 완료되었습니다.";

        } catch (Exception e) {
            log.error("이체 처리 중 오류 발생: ", e);
            throw new RuntimeException("이체 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 이메일 해시로 계좌 정보 조회 (해시 기반)
     */
    public TransferVO getAccountInfoByEmailHash(String emailHash) {
        // Mapper에서 TransferVO 바로 반환
        TransferVO accountInfo = transferMapper.getAccountInfoByEmailHash(emailHash);

        if (accountInfo == null) return null;

        // 계좌번호 복호화 처리
        try {
            if (accountInfo.getFromAccountNumber() != null) {
                String decrypted = aesUtil.decrypt(accountInfo.getFromAccountNumber());
                accountInfo.setFromAccountNumber(decrypted);
                accountInfo.setAccountNumber(decrypted); // EL용 필드에 복사
            }
        } catch (Exception e) {
            log.error("계좌번호 복호화 실패", e);
            accountInfo.setFromAccountNumber(null);
            accountInfo.setAccountNumber(null);
        }

        return accountInfo;
    }

    /**
     * 이메일 해시로 거래내역 조회 (해시 기반)
     */
    public List<TransferHistoryVO> getTransferHistoryByEmailHash(String emailHash) {
        List<TransferHistoryVO> historyList = transferMapper.getTransferHistoryByEmailHash(emailHash);

        if (historyList != null) {
            for (TransferHistoryVO h : historyList) {
                if (h.getAccountNumber() != null) {
                    try {
                        h.setAccountNumber(aesUtil.decrypt(h.getAccountNumber()));
                    } catch (Exception e) {
                        log.error("계좌번호 복호화 실패", e);
                    }
                }
                if (h.getOtherAccountNumber() != null) {
                    try {
                        h.setOtherAccountNumber(aesUtil.decrypt(h.getOtherAccountNumber()));
                    } catch (Exception e) {
                        log.error("상대방 계좌번호 복호화 실패", e);
                    }
                }
            }
        }

        return historyList;
    }

    /**
     * 특정 계좌의 거래내역 조회 (날짜 범위)
     */
    public List<TransferHistoryVO> getTransferHistoryByDateRange(int accountId, String startDate, String endDate) {
        return transferMapper.getTransferHistoryByDateRange(accountId, startDate, endDate);
    }

    // ===============================
    // 호환성용 메서드들 (이메일 기반)
    // ===============================

    /**
     * 이메일로 계좌 정보 조회 (호환성용)
     */
    @Deprecated
    public TransferVO getAccountInfoByEmail(String email) {
        String emailHash = CryptoUtil.generateEmailHash(email);
        return getAccountInfoByEmailHash(emailHash);
    }

    /**
     * 이메일로 거래내역 조회 (호환성용)
     */
    @Deprecated
    public List<TransferHistoryVO> getTransferHistoryByEmail(String email) {
        String emailHash = CryptoUtil.generateEmailHash(email);
        return getTransferHistoryByEmailHash(emailHash);
    }
}