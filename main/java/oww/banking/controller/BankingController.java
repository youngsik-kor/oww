package oww.banking.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import oww.banking.service.AccountService;
import oww.banking.service.SafeboxService;
import oww.banking.util.AESUtil;
import oww.banking.util.BankingJwtUtil;
import oww.banking.vo.AccountVO;
import oww.banking.vo.SafeboxVO;

@Controller
public class BankingController {

    @Autowired
    private AccountService accountService;
    @Autowired
    private SafeboxService safeboxService;
    @Autowired
    private AESUtil aesUtil;
    @Autowired
    private BankingJwtUtil jwtUtil;

    // 이메일 마스킹 유틸리티 메서드
    private String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***";
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }

    // JWT에서 사용자 정보 추출
    private BankingJwtUtil.TokenValidationResult extractUserFromJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new BankingJwtUtil.TokenValidationResult(false, "JWT 토큰이 없습니다", null, null, null, null);
        }

        String token = authHeader.substring(7);
        return jwtUtil.validateAndExtract(token);
    }

    /**
     * 계좌 정보 API (JWT 기반)
     */
    @GetMapping(value = "/api/account", produces = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAccountInfoApi(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        System.out.println("=== /api/account 요청 수신 (JWT 기반) ===");

        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다: " + tokenResult.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String userEmailHash = tokenResult.getUserEmailHash();
            String username = tokenResult.getUsername();

            System.out.println("JWT 인증 성공: " + username);

            response.put("success", true);
            response.put("userName", username);
            response.put("userEmailHash", userEmailHash);

            // 계좌 정보 조회 (해시 기반)
            AccountVO account = accountService.getAccountByEmailHash(userEmailHash);
            if (account != null) {
                response.put("hasAccount", true);
                response.put("accountNumber", account.getAccountNumber());
                response.put("balance", account.getBalance());
            } else {
                response.put("hasAccount", false);
            }

            // 세이프박스 정보 조회 (해시 기반)
            try {
                SafeboxVO safebox = safeboxService.getSafeboxByEmailHash(userEmailHash);
                if (safebox != null) {
                    response.put("hasSafebox", true);
                    response.put("safeboxNumber", "SB-" + safebox.getSafeboxId());
                    response.put("safeboxBalance", safebox.getBalance());
                } else {
                    response.put("hasSafebox", false);
                    response.put("safeboxBalance", 0);
                }
            } catch (Exception e) {
                System.out.println("세이프박스 정보 조회 오류: " + e.getMessage());
                response.put("hasSafebox", false);
                response.put("safeboxBalance", 0);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("계좌 정보 조회 중 오류: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "서버 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 계좌 생성 페이지 (JWT 기반)
     */
    @GetMapping("/createAccount")
    public String createAccount(Model model, HttpServletRequest request) {
        System.out.println("계좌 생성 페이지 호출됨 (JWT 기반)");

        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                System.out.println("JWT 인증 실패: " + tokenResult.getMessage());
                return "redirect:/main";
            }

            String userEmailHash = tokenResult.getUserEmailHash();
            String username = tokenResult.getUsername();
            
            System.out.println("JWT 인증 성공: " + username);
            
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("userName", username);
            model.addAttribute("userEmailHash", userEmailHash);

            // 이미 계좌가 있는지 확인 (해시 기반)
            if (accountService.existsByEmailHash(userEmailHash)) {
                model.addAttribute("hasExistingAccount", true);
                return "banking_createAccount";
            }

            return "banking_createAccount";
            
        } catch (Exception e) {
            System.out.println("계좌 생성 페이지 오류: " + e.getMessage());
            return "redirect:/main";
        }
    }

    /**
     * 이메일 인증번호 발송 (JWT 기반)
     */
    @PostMapping("/account/send-verification")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendEmailVerification(
            @RequestParam("email") String email,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다: " + tokenResult.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 이메일 유효성 검사
            if (email == null || !email.contains("@")) {
                response.put("success", false);
                response.put("message", "유효한 이메일 주소를 입력하세요.");
                return ResponseEntity.badRequest().body(response);
            }

            // 이미 계좌가 존재하는지 확인
            if (accountService.isAccountExists(email)) {
                response.put("success", false);
                response.put("message", "이미 계좌가 존재하는 이메일입니다.");
                return ResponseEntity.badRequest().body(response);
            }

            // 인증번호 발송
            String result = accountService.sendEmailVerification(email);

            response.put("success", true);
            response.put("message", result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("인증번호 발송 오류: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "인증번호 발송 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 이메일 인증번호 확인 (JWT 기반)
     */
    @PostMapping("/account/verify-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyEmailCode(
            @RequestParam("email") String email,
            @RequestParam("code") String code, 
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다: " + tokenResult.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 이메일 인증번호 확인
            boolean isValid = accountService.verifyEmailCode(email, code);

            if (isValid) {
                response.put("success", true);
                response.put("message", "인증번호가 확인되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "인증번호가 올바르지 않습니다.");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("인증번호 확인 오류: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "인증번호 확인 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 계좌 생성 (JWT 기반)
     */
    @PostMapping("/account/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createAccount(
            @RequestParam("name") String name,
            @RequestParam("email") String email, 
            @RequestParam("password") String password,
            @RequestParam("emailCode") String emailCode, 
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다: " + tokenResult.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 계좌 생성 서비스 호출
            String result = accountService.createAccount(name, email, password, emailCode);

            if (result.contains("성공적으로")) {
                response.put("success", true);
                response.put("message", result);
            } else {
                response.put("success", false);
                response.put("message", result);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("계좌 생성 오류: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "계좌 생성 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 계좌 생성 성공 페이지 (JWT 기반)
     */
    @GetMapping("/account/success")
    public String accountSuccess(Model model, HttpServletRequest request) {
        
        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                return "redirect:http://localhost:8201/banking/main";
            }
            
            String userEmailHash = tokenResult.getUserEmailHash();
            String username = tokenResult.getUsername();
            
            System.out.println("Success 페이지 - username: " + username);
            
            // 해시로 계좌 조회
            AccountVO newAccount = accountService.getAccountByEmailHash(userEmailHash);
            
            System.out.println("조회된 계좌: " + (newAccount != null ? newAccount.getAccountNumber() : "null"));
            
            model.addAttribute("userName", username);
            model.addAttribute("newAccount", newAccount);
            
            return "account_success";
            
        } catch (Exception e) {
            System.out.println("Success 페이지 오류: " + e.getMessage());
            return "redirect:http://localhost:8201/banking/main";
        }
    }
    
    /**
     * Banking 메인 페이지 (JWT 기반)
     */
    @GetMapping("/main")
    public String bankingMain(Model model, HttpServletRequest request) {
        
        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                System.out.println("JWT 인증 실패: " + tokenResult.getMessage());
                return "redirect:http://localhost:8201/";
            }

            String userEmailHash = tokenResult.getUserEmailHash();
            String userName = tokenResult.getUsername();
            
            model.addAttribute("userName", userName);
            model.addAttribute("userEmailHash", userEmailHash);
            model.addAttribute("active", "Main");

            // 계좌/세이프박스 조회 (해시 기반)
            AccountVO account = accountService.getAccountByEmailHash(userEmailHash);
            SafeboxVO safebox = safeboxService.getSafeboxByEmailHash(userEmailHash);

            // 계좌 정보 안전하게 처리
            long accountBalance = 0;
            String accountNumber = null;
            boolean hasAccount = false;
            
            if (account != null) {
                hasAccount = true;
                accountNumber = account.getAccountNumber();
                accountBalance = account.getBalance().longValue();
            }

            // 세이프박스 정보 안전하게 처리
            long safeboxBalance = 0;
            boolean hasSafebox = false;
            
            if (safebox != null && safebox.getBalance() != null) {
                hasSafebox = true;
                safeboxBalance = safebox.getBalance().longValue();
            }

            long totalAssets = accountBalance + safeboxBalance;

            // 모델에 안전하게 데이터 추가
            model.addAttribute("accountNumber", accountNumber);
            model.addAttribute("hasAccount", hasAccount);
            model.addAttribute("hasSafebox", hasSafebox);
            model.addAttribute("accountBalance", accountBalance);
            model.addAttribute("safeboxBalance", safeboxBalance);
            model.addAttribute("totalAssets", totalAssets);

            // 목표 대비 비율 계산
            int goalPercent = totalAssets > 0 ? (int)((safeboxBalance * 100) / totalAssets) : 0;
            model.addAttribute("goalPercent", goalPercent);

            return "banking_main";

        } catch (Exception e) {
            System.out.println("Banking main 페이지 오류: " + e.getMessage());
            return "redirect:http://localhost:8201/";
        }
    }

    /**
     * 헬스체크 엔드포인트 (인증 불필요)
     */
    @GetMapping("/health")
    @ResponseBody
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Banking Service is running on port 8203 - JWT Based");
    }
}