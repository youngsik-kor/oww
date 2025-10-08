package oww.banking.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SafeboxHistoryVO {

    private int paymentId;
    private int goalId;
    private BigDecimal amount;
    private LocalDate paymentDate;	
    private String userEmail;
    
    public SafeboxHistoryVO(int goalId, BigDecimal amount, LocalDate paymentDate) {
        this.goalId = goalId;
        this.amount = amount;
        this.paymentDate = paymentDate;
    }
    
}