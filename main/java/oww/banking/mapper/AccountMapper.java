package oww.banking.mapper;

import oww.banking.vo.AccountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AccountMapper {
    
    /**
     * 시퀀스 다음 값 가져오기
     * @return 다음 계좌 시퀀스 값
     */
    int getNextAccountSequence();
    
    /**
     * 계좌 생성
     * @param account 생성할 계좌 정보
     * @return 생성된 레코드 수
     */
    int createAccount(AccountVO account);
    
    /**
     * 이메일로 계좌 조회
     * @param userEmail 사용자 이메일
     * @return 계좌 정보
     */
    AccountVO findAccountByEmail(String userEmail);
    
    /**
     * 이메일로 계좌 존재 여부 확인
     * @param userEmail 사용자 이메일
     * @return 계좌 존재 여부
     */
    boolean existsByEmail(String userEmail);
    
    /**
     * 계좌번호로 계좌 존재 여부 확인
     * @param accountNumber 계좌번호
     * @return 계좌 존재 여부
     */
    boolean existsByAccountNumber(String accountNumber);
    
    /**
     * 계좌 ID로 계좌 조회
     * @param accountId 계좌 ID
     * @return 계좌 정보
     */
    AccountVO findAccountById(int accountId);
    
    /**
     * 계좌 잔액 업데이트
     * @param accountId 계좌 ID
     * @param balance 새로운 잔액
     * @return 업데이트된 레코드 수
     */
    int updateBalance(@Param("accountId") int accountId, @Param("balance") BigDecimal balance);

    
    /**
     * 계좌 비밀번호 확인
     * @param accountId 계좌 ID
     * @param password 비밀번호
     * @return 비밀번호 일치 여부
     */
    boolean verifyPassword(@Param("accountId") int accountId, @Param("password") String password);
    
    /**
     * 모든 계좌 조회 (관리자용)
     * @return 모든 계좌 목록
     */
    List<AccountVO> findAllAccounts();
    
    /**
     * 계좌 삭제
     * @param accountId 삭제할 계좌 ID
     * @return 삭제된 레코드 수
     */
    int deleteAccount(int accountId);
    
    AccountVO findAccountByUserNo(int userNo);
    // 해시 검색 메서드 추가
    AccountVO findAccountByEmailHash(@Param("emailHash") String emailHash);
    boolean existsByEmailHash(@Param("emailHash") String emailHash);

    
}