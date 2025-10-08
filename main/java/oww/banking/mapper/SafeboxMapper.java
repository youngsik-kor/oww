package oww.banking.mapper;

import oww.banking.vo.SafeboxVO;
import oww.banking.vo.SafeboxGoalVO;
import oww.banking.vo.SafeboxHistoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface SafeboxMapper {

    // ================= Safebox =================

    /**
     * 세이프박스 생성
     */
    void createSafebox(SafeboxVO safebox);

    /**
     * emailHash 기반 세이프박스 조회
     */
    SafeboxVO findSafeboxByEmailHash(@Param("emailHash") String emailHash);

    /**
     * safeboxId 기반 조회
     */
    SafeboxVO findSafeboxBySafeboxId(@Param("safeboxId") int safeboxId);

    /**
     * 세이프박스 잔액 업데이트
     * params: "safeboxId", "balance"
     */
    void updateSafeboxBalance(Map<String, Object> params);

    /**
     * emailHash로 세이프박스 존재 여부 확인
     */
    boolean existsByEmailHash(@Param("emailHash") String emailHash);


    // ================= SafeboxGoal =================

    /**
     * 세이프박스 목표 생성
     */
    void createSafeboxGoal(SafeboxGoalVO goal);

    /**
     * safeboxId 기반 목표 조회
     */
    List<SafeboxGoalVO> findGoalsBySafeboxId(@Param("safeboxId") int safeboxId);

    /**
     * goalId 기반 목표 조회
     */
    SafeboxGoalVO findGoalById(@Param("goalId") int goalId);


    // ================= SafeboxHistory =================

    /**
     * 세이프박스 저축 내역 생성
     */
    void createSafeboxHistory(SafeboxHistoryVO history);

    /**
     * goalId 기반 저축 내역 조회
     */
    List<SafeboxHistoryVO> findHistoryByGoalId(@Param("goalId") int goalId);

    /**
     * goalId 기반 총 저축 금액 조회
     */
    BigDecimal getTotalSavedAmountByGoalId(@Param("goalId") int goalId);

    /**
     * emailHash 기반으로 사용자 전체 저축 내역 조회
     */
    List<SafeboxHistoryVO> findHistoryByUserEmailHash(@Param("emailHash") String emailHash);

    /**
     * emailHash 기반 총 자산 조회 (계좌 + 세이프박스 합산)
     * 필요 시 Mapper XML에서 JOIN 사용
     */
    BigDecimal getTotalAssetsByEmailHash(@Param("emailHash") String emailHash);
}
