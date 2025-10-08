package oww.banking.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import oww.banking.vo.UserVO;

@Mapper
public interface UserMapper {

    /**
     * 해시로 사용자 조회
     * @param emailHash 사용자 이메일 해시
     * @return 사용자 정보
     */
    UserVO findByEmailHash(@Param("emailHash") String emailHash);

    /**
     * 해시로 사용자 존재 여부 확인
     * @param emailHash 사용자 이메일 해시
     * @return 존재 여부 (true/false)
     */
    boolean existsByEmailHash(@Param("emailHash") String emailHash);

    /**
     * 이메일로 사용자 직접 조회 (필요시만 사용)
     * @param email 사용자 이메일
     * @return 사용자 정보
     */
    UserVO findByEmail(@Param("email") String email);

    /**
     * 이메일로 사용자 존재 여부 확인 (필요시만 사용)
     * @param email 사용자 이메일
     * @return 존재 여부 (true/false)
     */
    boolean existsByEmail(@Param("email") String email);

    /**
     * 새 사용자 생성
     * @param user 사용자 정보
     * @return 생성된 행 수
     */
    int createUser(UserVO user);

    /**
     * 사용자 정보 업데이트
     * @param user 사용자 정보
     * @return 업데이트된 행 수
     */
    int updateUser(UserVO user);
}