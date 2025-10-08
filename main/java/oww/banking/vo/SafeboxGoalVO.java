package oww.banking.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SafeboxGoalVO {


	private int goalId;
	private int safeboxId;
	private String title;
	private BigDecimal targetAmount;
	private LocalDate startDate;
	private LocalDate endDate;
	private String paymentType; // "daily" or "monthly"
	private LocalDateTime createdAt;

	public SafeboxGoalVO(int safeboxId, String title, BigDecimal targetAmount, LocalDate startDate, LocalDate endDate,
			String paymentType) {
		this.safeboxId = safeboxId;
		this.title = title;
		this.targetAmount = targetAmount;
		this.startDate = startDate;
		this.endDate = endDate;
		this.paymentType = paymentType;
	}

}