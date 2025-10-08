package oww.banking.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class AESUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding"; // 🔥 검색 가능한 ECB 모드

    // TODO: 나중에 설정파일로 이동 (지금은 테스트용)
    private static final String SECRET_KEY = "MySecretKey12345MySecretKey12345"; // 32바이트 키

    /**
     * ECB 모드 암호화 - 같은 평문은 항상 같은 암호문 생성
     */
    public String encrypt(String plaintext) throws Exception {
        try {
            // 1. 32바이트 키 생성
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);

            // 2. Cipher 설정 (ECB 모드는 IV 불필요)
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            // 3. 암호화 실행
            byte[] encryptedData = cipher.doFinal(plaintext.getBytes("UTF-8"));

            // 4. Base64로 인코딩
            return Base64.getEncoder().encodeToString(encryptedData);

        } catch (Exception e) {
            throw new Exception("암호화 실패: " + e.getMessage());
        }
    }

    /**
     * ECB 모드 복호화
     */
    public String decrypt(String encryptedData) throws Exception {
        try {
            // 1. Base64 디코딩
            byte[] data;
            try {
                // 1차 시도: 표준 Base64
                data = Base64.getDecoder().decode(encryptedData);
            } catch (IllegalArgumentException ex) {
                // 2차 시도: URL-safe Base64
                data = Base64.getUrlDecoder().decode(encryptedData);
            }

            // 2. 키 생성
            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);

            // 3. Cipher 설정 (ECB 모드는 IV 불필요)
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            // 4. 복호화 실행
            byte[] decryptedData = cipher.doFinal(data);
            return new String(decryptedData, "UTF-8");

        } catch (Exception e) {
            throw new Exception("복호화 실패: " + e.getMessage());
        }
    }
}