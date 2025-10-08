package oww.banking.scheduler;

import oww.banking.service.SafeboxService;
import oww.banking.vo.SafeboxGoalVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class SafeboxScheduler {

    @Autowired
    private SafeboxService safeboxService;

    /**
     * 매일 자정에 실행되는 자동 저축 스케줄러
     */
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정
    public void executeAutoSaving() {
        try {
            // 모든 활성 저축 목표 조회 (이 메서드는 Service에 추가 구현 필요)
            // List<SafeboxGoalVO> activeGoals = safeboxService.getActiveGoals();
            
            // for (SafeboxGoalVO goal : activeGoals) {
            //     if (shouldExecuteToday(goal)) {
            //         BigDecimal amount = calculateDailyAmount(goal);
            //         safeboxService.executeAutoSaving(goal.getGoalId(), amount);
            //     }
            // }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean shouldExecuteToday(SafeboxGoalVO goal) {
        LocalDate today = LocalDate.now();
        return !today.isBefore(goal.getStartDate()) && !today.isAfter(goal.getEndDate());
    }

    private BigDecimal calculateDailyAmount(SafeboxGoalVO goal) {
        LocalDate today = LocalDate.now();
        long remainingDays = ChronoUnit.DAYS.between(today, goal.getEndDate());
        
        if (remainingDays <= 0) {
            return BigDecimal.ZERO;
        }

        // 현재까지 저축한 금액을 빼고 남은 금액을 남은 일수로 나누기
        BigDecimal savedAmount = safeboxService.getTotalSavedAmount(goal.getGoalId());
        BigDecimal remainingAmount = goal.getTargetAmount().subtract(savedAmount);
        
        return remainingAmount.divide(BigDecimal.valueOf(remainingDays), 2, BigDecimal.ROUND_UP);
    }
}