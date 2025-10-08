package oww.banking.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import oww.banking.service.AccountService;
import oww.banking.service.SafeboxService;
import oww.banking.service.TransferService;
import oww.banking.util.AESUtil;
import oww.banking.util.BankingJwtUtil;
import oww.banking.vo.AccountVO;
import oww.banking.vo.SafeboxVO;
import oww.banking.vo.TransferVO;
import oww.banking.vo.TransferHistoryVO;
import jakarta.servlet.http.Cookie;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Controller
public class TransferController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private SafeboxService safeboxService;

    @Autowired
    private AESUtil aesUtil;

    @Autowired
    private BankingJwtUtil jwtUtil;

    // JWT에서 사용자 정보 추출
    private BankingJwtUtil.TokenValidationResult extractUserFromJwt(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 확인
        String authHeader = request.getHeader("Authorization");
        String token = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            // 2. 쿠키에서 토큰 확인 (헤더에 없을 경우)
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt-token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }
        
        if (token == null) {
            return new BankingJwtUtil.TokenValidationResult(false, "JWT 토큰이 없습니다", null, null, null, null);
        }
        
        return jwtUtil.validateAndExtract(token);
    }

    @PostMapping("/check-account")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkAccount(
            @RequestParam String accountNumber,
            HttpServletRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다: " + tokenResult.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Map<String, Object> accountInfo = transferService.getAccountInfoByNumber(accountNumber);
            
            if (accountInfo != null) {
                response.put("success", true);
                response.put("accountInfo", accountInfo);
            } else {
                response.put("success", false);
                response.put("message", "존재하지 않는 계좌번호입니다.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("계좌 확인 오류: " + e.getMessage());
            response.put("success", false);
            response.put("message", "계좌 확인 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /** 이체 1단계 - 내 계좌 정보 표시 (JWT 기반) */
    @GetMapping("/transfer_1")
    public String transferStep1(Model model, HttpServletRequest request) {
        
        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                System.out.println("JWT 인증 실패: " + tokenResult.getMessage());
                return "redirect:http://localhost:8201/";
            }

            String username = tokenResult.getUsername();
            String userEmailHash = tokenResult.getUserEmailHash();

            // 계좌 조회 (해시 기반)
            TransferVO accountInfo = transferService.getAccountInfoByEmailHash(userEmailHash);
            SafeboxVO safebox = safeboxService.getSafeboxByEmailHash(userEmailHash);

            BigDecimal accountBalance = accountInfo != null && accountInfo.getAmount() != null
                    ? accountInfo.getAmount() : BigDecimal.ZERO;
            BigDecimal safeboxBalance = safebox != null && safebox.getBalance() != null
                    ? safebox.getBalance() : BigDecimal.ZERO;
            BigDecimal totalAssets = accountBalance.add(safeboxBalance);
            int goalPercent = totalAssets.compareTo(BigDecimal.ZERO) > 0
                    ? safeboxBalance.multiply(BigDecimal.valueOf(100)).divide(totalAssets, 0, BigDecimal.ROUND_DOWN).intValue()
                    : 0;

            model.addAttribute("account", accountInfo);
            model.addAttribute("accountNumber", accountInfo != null ? accountInfo.getFromAccountNumber() : null);
            model.addAttribute("balance", accountBalance);
            model.addAttribute("accountBalance", accountBalance);
            model.addAttribute("safeboxBalance", safeboxBalance);
            model.addAttribute("totalAssets", totalAssets);
            model.addAttribute("hasAccount", accountInfo != null);
            model.addAttribute("hasSafebox", safebox != null);
            model.addAttribute("userName", username);
            model.addAttribute("userEmailHash", userEmailHash);
            model.addAttribute("goalPercent", goalPercent);

            return "transfer/banking_transfer_1";

        } catch (Exception e) {
            System.out.println("이체 1단계 페이지 오류: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "계좌 정보 조회 중 오류가 발생했습니다.");
            return "redirect:http://localhost:8201/banking/main";
        }
    }

    /** 이체 2단계 (JWT 기반) */
    @GetMapping("/transfer_2")
    public String transferStep2(Model model, HttpServletRequest request) {
        
        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                return "redirect:/";
            }

            String username = tokenResult.getUsername();
            String userEmailHash = tokenResult.getUserEmailHash();

            TransferVO accountInfo = transferService.getAccountInfoByEmailHash(userEmailHash);
            SafeboxVO safebox = safeboxService.getSafeboxByEmailHash(userEmailHash);

            BigDecimal accountBalance = accountInfo != null && accountInfo.getAmount() != null
                    ? accountInfo.getAmount() : BigDecimal.ZERO;
            BigDecimal safeboxBalance = safebox != null && safebox.getBalance() != null
                    ? safebox.getBalance() : BigDecimal.ZERO;
            BigDecimal totalAssets = accountBalance.add(safeboxBalance);
            int goalPercent = totalAssets.compareTo(BigDecimal.ZERO) > 0
                    ? safeboxBalance.multiply(BigDecimal.valueOf(100)).divide(totalAssets, 0, BigDecimal.ROUND_DOWN).intValue()
                    : 0;

            model.addAttribute("account", accountInfo);
            model.addAttribute("accountNumber", accountInfo != null ? accountInfo.getFromAccountNumber() : null);
            model.addAttribute("balance", accountBalance);
            model.addAttribute("accountBalance", accountBalance);
            model.addAttribute("safeboxBalance", safeboxBalance);
            model.addAttribute("totalAssets", totalAssets);
            model.addAttribute("hasAccount", accountInfo != null);
            model.addAttribute("hasSafebox", safebox != null);
            model.addAttribute("userName", username);
            model.addAttribute("userEmailHash", userEmailHash);
            model.addAttribute("goalPercent", goalPercent);

            return "transfer/banking_transfer_2";

        } catch (Exception e) {
            System.out.println("이체 2단계 페이지 오류: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "페이지 로드 중 오류가 발생했습니다.");
            return "redirect:http://localhost:8201/banking/main";
        }
    }
    
    @PostMapping("/transfer")
    @ResponseBody  // 추가
    public Map<String, Object> processTransfer(
        @RequestParam("toAccountNumber") String toAccountNumber,
        @RequestParam("amount") String amountStr,
        @RequestParam("memo") String memo,
        @RequestParam("password") String password,
        @RequestParam(value = "recipientName", required = false) String recipientName,
        HttpServletRequest request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다.");
                return response;
            }
            
            String userEmailHash = tokenResult.getUserEmailHash();
            TransferVO fromAccount = transferService.getAccountInfoByEmailHash(userEmailHash);
            
            if (fromAccount == null) {
                response.put("success", false);
                response.put("message", "계좌 정보를 찾을 수 없습니다.");
                return response;
            }
            
            BigDecimal amount = new BigDecimal(amountStr.replace(",", ""));
            String result = transferService.processTransferByEmailHash(
                userEmailHash, toAccountNumber, amount, memo, password
            );
            
            if (result == null || (!result.contains("오류") && !result.contains("실패") && !result.contains("부족"))) {
                // 성공
                response.put("success", true);
                response.put("message", "이체가 완료되었습니다.");
                response.put("recipientName", recipientName);
                response.put("toAccountNumber", toAccountNumber);
                response.put("amount", amount.intValue());
                response.put("memo", memo);
                return response;
            } else {
                // 실패
                response.put("success", false);
                response.put("message", result);
                return response;
            }
            
        } catch (Exception e) {
            System.out.println("이체 처리 오류: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "이체 처리 중 오류가 발생했습니다: " + e.getMessage());
            return response;
        }
    }
    
    /** 이체 3단계 - 완료 페이지 (JWT 기반) */
    @GetMapping("/transfer_3")
    public String transferStep3(@RequestParam(required = false) String recipientName,
                                @RequestParam(required = false) String toAccountNumber,
                                @RequestParam(required = false) Integer amount,
                                @RequestParam(required = false) String memo,
                                Model model,
                                HttpServletRequest request) {

        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                return "redirect:http://localhost:8201/";
            }

            String username = tokenResult.getUsername();
            String userEmailHash = tokenResult.getUserEmailHash();

            TransferVO accountInfo = transferService.getAccountInfoByEmailHash(userEmailHash);
            SafeboxVO safebox = safeboxService.getSafeboxByEmailHash(userEmailHash);

            BigDecimal accountBalance = accountInfo != null && accountInfo.getAmount() != null
                    ? accountInfo.getAmount() : BigDecimal.ZERO;
            BigDecimal safeboxBalance = safebox != null && safebox.getBalance() != null
                    ? safebox.getBalance() : BigDecimal.ZERO;
            BigDecimal totalAssets = accountBalance.add(safeboxBalance);
            int goalPercent = totalAssets.compareTo(BigDecimal.ZERO) > 0
                    ? safeboxBalance.multiply(BigDecimal.valueOf(100)).divide(totalAssets, 0, BigDecimal.ROUND_DOWN).intValue()
                    : 0;

            String transferTime = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            model.addAttribute("recipientName", recipientName);
            model.addAttribute("toAccountNumber", toAccountNumber);
            model.addAttribute("amount", amount);
            model.addAttribute("memo", memo);
            model.addAttribute("transferTime", transferTime);

            model.addAttribute("account", accountInfo);
            model.addAttribute("accountNumber", accountInfo != null ? accountInfo.getFromAccountNumber() : null);
            model.addAttribute("balance", accountBalance);
            model.addAttribute("accountBalance", accountBalance);
            model.addAttribute("safeboxBalance", safeboxBalance);
            model.addAttribute("totalAssets", totalAssets);
            model.addAttribute("hasAccount", accountInfo != null);
            model.addAttribute("hasSafebox", safebox != null);
            model.addAttribute("userName", username);
            model.addAttribute("userEmailHash", userEmailHash);
            model.addAttribute("goalPercent", goalPercent);

            return "transfer/banking_transfer_3";

        } catch (Exception e) {
            System.out.println("이체 3단계 페이지 오류: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "페이지 로드 중 오류가 발생했습니다.");
            return "redirect:http://localhost:8201/banking/main";
        }
    }

    /** 거래내역 (JWT 기반) */
    @GetMapping("/history")
    public String transferHistory(Model model, HttpServletRequest request) {
        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                return "redirect:http://localhost:8201/";
            }

            String userEmailHash = tokenResult.getUserEmailHash();

            TransferVO accountInfo = transferService.getAccountInfoByEmailHash(userEmailHash);
            if (accountInfo == null) {
                model.addAttribute("errorMessage", "계좌 정보를 찾을 수 없습니다.");
                return "redirect:/banking/main";
            }

            List<TransferHistoryVO> history = transferService.getTransferHistoryByEmailHash(userEmailHash);

            model.addAttribute("account", accountInfo);
            model.addAttribute("history", history);
            
            // JavaScript에서 사용할 수 있도록 JSON 문자열로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime 같은 경우 직렬화 지원
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            String historyJson = objectMapper.writeValueAsString(history);
            model.addAttribute("historyJson", historyJson);

            return "transfer/transfer_history";

        } catch (Exception e) {
            System.out.println("거래내역 조회 오류: " + e.getMessage());
            model.addAttribute("errorMessage", "거래내역 조회 중 오류가 발생했습니다.");
            return "redirect:http://localhost:8201/banking/main";
        }
    }
    
    @GetMapping("/transfer-test")
    @ResponseBody
    public String transferTest() {
        return "Transfer controller works!";
    }

    @PostMapping("/transfer-test")
    @ResponseBody
    public String transferTestPost() {
        return "Transfer POST works!";
    }

    /** AJAX 거래내역 조회 (JWT 기반) */
    @GetMapping("/history-data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTransferHistoryData(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다: " + tokenResult.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String userEmailHash = tokenResult.getUserEmailHash();

            TransferVO accountInfo = transferService.getAccountInfoByEmailHash(userEmailHash);

            if (accountInfo == null) {
                response.put("success", false);
                response.put("message", "계좌 정보를 찾을 수 없습니다.");
                return ResponseEntity.badRequest().body(response);
            }

            List<TransferHistoryVO> history;
            if (startDate != null && endDate != null) {
                history = transferService.getTransferHistoryByDateRange(accountInfo.getFromAccountId(), startDate, endDate);
            } else {
                history = transferService.getTransferHistoryByEmailHash(userEmailHash);
            }

            response.put("success", true);
            response.put("history", history);
            response.put("account", accountInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("AJAX 거래내역 조회 오류: " + e.getMessage());
            response.put("success", false);
            response.put("message", "거래내역 조회 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /** 계좌번호 포맷팅 헬퍼 */
    private String formatAccountNumber(String accountNumber) {
        if (accountNumber != null && accountNumber.length() >= 12) {
            String cleanNumber = accountNumber.replaceAll("[^0-9]", "");
            if (cleanNumber.length() == 14) {
                return cleanNumber.substring(0, 4) + "-" + cleanNumber.substring(4, 8) + "-" + cleanNumber.substring(8);
            }
        }
        return accountNumber;
    }
    
   
    
}