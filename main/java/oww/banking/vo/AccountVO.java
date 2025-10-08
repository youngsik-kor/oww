package oww.banking.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AccountVO {
    private int accountId;
    private String accountNumber;   // AES-256-GCM 암호화된 값, 서비스에서 복호화/암호화 처리
    private String userEmail;
    private BigDecimal balance;     // String → BigDecimal로 변경하는 게 금액 계산 편리
    private LocalDateTime createdAt;  
    private String accountPassword; // bcrypt 해시, 서비스에서 검증
}
