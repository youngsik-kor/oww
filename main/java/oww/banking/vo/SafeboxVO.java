package oww.banking.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SafeboxVO {
	    private int safeboxId;
	    private String userEmail;
	    private String emailHash;
	    private BigDecimal balance;
	    private LocalDateTime createdAt;
	    
	    public SafeboxVO(String userEmail, BigDecimal balance) {
	        this.userEmail = userEmail;
	        this.balance = balance;
	    }
	    
	    public SafeboxVO(String userEmail, String emailHash, BigDecimal balance) {
	        this.userEmail = userEmail;
	        this.emailHash = emailHash;   // ← emailHash 초기화 생성자
	        this.balance = balance;
	    }
	    
	    
}