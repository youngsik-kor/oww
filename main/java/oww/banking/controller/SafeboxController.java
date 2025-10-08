package oww.banking.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import oww.banking.service.AccountService;
import oww.banking.service.SafeboxService;
import oww.banking.service.TransferService;
import oww.banking.util.AESUtil;
import oww.banking.util.BankingJwtUtil;
import oww.banking.vo.AccountVO;
import oww.banking.vo.SafeboxGoalVO;
import oww.banking.vo.SafeboxHistoryVO;
import oww.banking.vo.SafeboxVO;
import jakarta.servlet.http.Cookie;

@Controller
@RequestMapping("/safebox")
public class SafeboxController {

    @Autowired
    private SafeboxService safeboxService;

    @Autowired
    private AccountService accountService;



    @Autowired
    private AESUtil aesUtil;

    @Autowired
    private BankingJwtUtil jwtUtil;

    
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
    

    /**
     * 세이프박스 기본 페이지 (리다이렉트)
     */
    @GetMapping("")
    public String safeboxIndex() {
        return "redirect:http://localhost:8201/banking/safebox/main";
    }

    /**
     * 세이프박스 메인 페이지 (JWT 기반)
     */
    @GetMapping("/main")
    public String safeboxMain(Model model, HttpServletRequest request) {
        
        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                System.out.println("JWT 인증 실패: " + tokenResult.getMessage());
                return "redirect:http://localhost:8201/";
            }

            String username = tokenResult.getUsername();
            String userEmailHash = tokenResult.getUserEmailHash();

            // 계좌/세이프박스 조회 (해시 기반)
            AccountVO account = accountService.getAccountByEmailHash(userEmailHash);
            SafeboxVO safebox = safeboxService.getSafeboxByEmailHash(userEmailHash);

         // 계좌번호 복호화 
            String accountNumber = null;
            if (account != null && account.getAccountNumber() != null) {
                try {
                    accountNumber = aesUtil.decrypt(account.getAccountNumber());
                } catch (Exception e) {
                    System.out.println("계좌번호 복호화 실패: " + e.getMessage());
                    accountNumber = null;
                }
            }

            // 잔액 계산
            BigDecimal accountBalance = account != null ? account.getBalance() : BigDecimal.ZERO;
            BigDecimal safeboxBalance = safebox != null && safebox.getBalance() != null ? safebox.getBalance() : BigDecimal.ZERO;
            BigDecimal totalAssets = accountBalance.add(safeboxBalance);

            int goalPercent = totalAssets.compareTo(BigDecimal.ZERO) > 0
                    ? safeboxBalance.multiply(BigDecimal.valueOf(100)).divide(totalAssets, 0, BigDecimal.ROUND_DOWN).intValue()
                    : 0;

            // 정기저금 목표 조회 (해시 기반)
            List<SafeboxGoalVO> goals = safeboxService.getSavingGoals(userEmailHash);

            // 모델에 데이터 추가
            model.addAttribute("account", account);
            model.addAttribute("safebox", safebox);
            model.addAttribute("accountNumber", accountNumber);
            model.addAttribute("balance", accountBalance);
            model.addAttribute("safeboxBalance", safeboxBalance);
            model.addAttribute("totalAssets", totalAssets);
            model.addAttribute("hasAccount", account != null);
            model.addAttribute("hasSafebox", safebox != null);
            model.addAttribute("userName", username);
            model.addAttribute("userEmailHash", userEmailHash);
            model.addAttribute("goalPercent", goalPercent);
            model.addAttribute("goals", goals);

            return "safebox/main";

        } catch (Exception e) {
            System.out.println("세이프박스 메인 페이지 오류: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "세이프박스 정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:http://localhost:8201/banking/main";
        }
    }

    /**
     * 세이프박스 금액 설정 (JWT 기반)
     */
    @PostMapping("/setAmount")
    @ResponseBody
    public Map<String, Object> setSafeboxAmount(@RequestParam("amount") BigDecimal amount,
                                                HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                result.put("success", false);
                result.put("message", "인증이 필요합니다: " + tokenResult.getMessage());
                return result;
            }

            String userEmailHash = tokenResult.getUserEmailHash();

            boolean success = safeboxService.setSafeboxAmount(userEmailHash, amount);
            AccountVO account = accountService.getAccountByEmailHash(userEmailHash);
            SafeboxVO safebox = safeboxService.getSafeboxByEmailHash(userEmailHash);

            result.put("success", success);
            result.put("message", success ? "세이프박스 금액이 설정되었습니다." : "세이프박스 설정에 실패했습니다.");
            result.put("accountBalance", account != null ? account.getBalance() : BigDecimal.ZERO);
            result.put("safeboxBalance", safebox != null ? safebox.getBalance() : BigDecimal.ZERO);

        } catch (Exception e) {
            System.out.println("세이프박스 금액 설정 오류: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 정기저금통 설정 (JWT 기반)
     */
    @PostMapping("/createGoal")
    @ResponseBody
    public Map<String, Object> createSavingGoal(@RequestParam("goalAmount") BigDecimal goalAmount,
                                                @RequestParam("startDate") String startDateStr,
                                                @RequestParam("endDate") String endDateStr,
                                                @RequestParam("cycle") String cycle,
                                                HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                result.put("success", false);
                result.put("message", "인증이 필요합니다: " + tokenResult.getMessage());
                return result;
            }

            String userEmailHash = tokenResult.getUserEmailHash();

            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);

            String title = "정기저금 목표 - " + goalAmount + "원";
            boolean success = safeboxService.createSavingGoal(userEmailHash, title, goalAmount, startDate, endDate, cycle);

            result.put("success", success);
            result.put("message", success ? "정기저금통이 설정되었습니다." : "정기저금통 설정에 실패했습니다.");

        } catch (Exception e) {
            System.out.println("정기저금통 설정 오류: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "오류가 발생했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * 저축 목표 상세 페이지 (JWT 기반)
     */
    @GetMapping("/goal/{goalId}")
    public String goalDetail(@PathVariable("goalId") int goalId, Model model,
                             HttpServletRequest request) {
        
        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                return "redirect:http://localhost:8201/";
            }

            String userEmailHash = tokenResult.getUserEmailHash();

            List<SafeboxHistoryVO> history = safeboxService.getSavingHistory(goalId);
            BigDecimal totalSaved = safeboxService.getTotalSavedAmount(goalId);

            model.addAttribute("history", history);
            model.addAttribute("totalSaved", totalSaved);
            model.addAttribute("goalId", goalId);

            return "safebox/goal-detail";

        } catch (Exception e) {
            System.out.println("저축 목표 상세 페이지 오류: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "저축 목표 정보를 불러오는 중 오류가 발생했습니다.");
            return "error";
        }
    }


/*
     * 세이프박스 거래내역 조회 (JWT 기반) - 수정된 버전
     */
    @GetMapping("/history")
    public String safeboxHistory(@RequestParam(value = "goalId", required = false) Integer goalId, 
                                 Model model, HttpServletRequest request) {
        
        try {
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);
            
            if (!tokenResult.isValid()) {
                return "redirect:http://localhost:8201/";
            }

            String userEmailHash = tokenResult.getUserEmailHash();
            List<SafeboxHistoryVO> history;

            if (goalId != null) {
                history = safeboxService.getSavingHistory(goalId);
            } else {
                history = safeboxService.getFullSavingHistory(userEmailHash);
            }

            model.addAttribute("history", history);
            return "safebox/history";
            
        } catch (Exception e) {
            System.out.println("세이프박스 거래내역 조회 오류: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "세이프박스 내역 조회 중 오류가 발생했습니다.");
            return "error";
        }
    }
    
    /**
     * 세이프박스/계좌 잔액 정보 조회 (AJAX용, JWT 기반)
     */
    @GetMapping("/info")
    @ResponseBody
    public Map<String, Object> getSafeboxInfo(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            // JWT 검증
            BankingJwtUtil.TokenValidationResult tokenResult = extractUserFromJwt(request);

            if (!tokenResult.isValid()) {
                result.put("success", false);
                result.put("message", "인증이 필요합니다: " + tokenResult.getMessage());
                return result;
            }

            String userEmailHash = tokenResult.getUserEmailHash();

            // 계좌 & 세이프박스 조회
            AccountVO account = accountService.getAccountByEmailHash(userEmailHash);
            SafeboxVO safebox = safeboxService.getSafeboxByEmailHash(userEmailHash);

            BigDecimal accountBalance = account != null ? account.getBalance() : BigDecimal.ZERO;
            BigDecimal safeboxBalance = safebox != null && safebox.getBalance() != null ? safebox.getBalance() : BigDecimal.ZERO;
            BigDecimal totalAssets = accountBalance.add(safeboxBalance);

            // 응답 데이터
            result.put("success", true);
            result.put("accountBalance", accountBalance);
            result.put("safeboxBalance", safeboxBalance);
            result.put("totalAssets", totalAssets);

        } catch (Exception e) {
            System.out.println("세이프박스 정보 조회 오류: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "세이프박스 정보를 불러오는 중 오류가 발생했습니다.");
        }

        return result;
    }

    
}