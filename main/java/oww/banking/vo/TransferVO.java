package oww.banking.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferVO {
	private String accountNumber;
    private int transferId;
    private int fromAccountId;
    private int toAccountId;
    private BigDecimal amount;
    private String memo;
    private LocalDateTime transferDate;
    private BigDecimal balance;
    
    // 추가 정보 (조인용)
    private String fromAccountNumber;
    private String toAccountNumber;
    private String fromUserName;
    private String toUserName;
}