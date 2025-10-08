package oww.banking.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import oww.banking.vo.TransferVO;
import oww.banking.vo.TransferHistoryVO;

public interface TransferMapper {

    /* ===== 기존 계좌번호 기반 ===== */
	Integer findAccountIdByNumber(@Param("accountNumber") String accountNumber);

    BigDecimal getBalanceByAccountId(Integer accountId);

    boolean checkAccountPassword(@Param("accountId") Integer accountId, 
                                 @Param("password") String password);

    void updateBalance(@Param("accountId") Integer accountId, 
                       @Param("balance") BigDecimal balance);

    void insertTransfer(TransferVO transfer);

    void insertTransferHistory(TransferHistoryVO history);

    List<TransferHistoryVO> getTransferHistoryByAccountId(Integer accountId);

    Map<String, Object> getAccountInfoByNumber(String accountNumber);

    List<TransferHistoryVO> getTransferHistoryByDateRange(@Param("accountId") Integer accountId,
                                                          @Param("startDate") String startDate,
                                                          @Param("endDate") String endDate);

    /* ===== 이메일 해시 기반 메서드 ===== */
    // 이메일 해시로 계좌 ID 조회
    Integer findAccountIdByEmailHash(@Param("emailHash") String emailHash);

 // 이메일 해시로 계좌 정보 조회 (TransferVO 바로 반환)
    TransferVO getAccountInfoByEmailHash(@Param("emailHash") String emailHash);

    String getAccountPassword(@Param("accountId") Integer accountId);

    // 이메일 해시로 거래내역 조회
    List<TransferHistoryVO> getTransferHistoryByEmailHash(@Param("emailHash") String emailHash);
}
