package oww.banking.config;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import oww.banking.service.AccountService;
import oww.banking.service.SafeboxService;
import oww.banking.util.AESUtil;
import oww.banking.vo.AccountVO;
import oww.banking.vo.SafeboxVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalModelAdvice {

    @Autowired
    private AccountService accountService;

    @Autowired
    private SafeboxService safeboxService;

    @Autowired
    private AESUtil aesUtil;

    @ModelAttribute
    public void addGlobalAttributes(Model model, HttpSession session, Authentication authentication, HttpServletRequest request) {

        // 기존 세션 데이터 추가
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("userEmail", session.getAttribute("userEmail"));
        model.addAttribute("totalAssets", session.getAttribute("totalAssets"));
        model.addAttribute("safeboxBalance", session.getAttribute("safeboxBalance"));
        model.addAttribute("goalPercent", session.getAttribute("goalPercent"));

        if (authentication != null && authentication.isAuthenticated()) {
            try {
                String emailHash = request.getHeader("x-user-email-hash"); // 이메일 해시 사용

                if (emailHash == null || emailHash.isEmpty()) {
                    setDefaultGlobalAttributes(model);
                    return;
                }

                // 계좌 조회 (이메일 해시 기반)
                AccountVO account = accountService.getAccountByEmailHash(emailHash);
                boolean hasAccount = (account != null);

                // 세이프박스 조회 (이메일 해시 기반)
                SafeboxVO safebox = safeboxService.getSafeboxByEmailHash(emailHash);
                boolean hasSafebox = (safebox != null);

                // 계좌번호 복호화
                String decryptedAccountNumber = null;
                if (hasAccount && account.getAccountNumber() != null) {
                    try {
                        decryptedAccountNumber = aesUtil.decrypt(account.getAccountNumber());
                    } catch (Exception e) {
                        System.out.println("계좌번호 복호화 실패: " + e.getMessage());
                    }
                }

                // 잔액은 BigDecimal 그대로 사용
                BigDecimal accountBalance = hasAccount && account.getBalance() != null
                        ? account.getBalance()
                        : BigDecimal.ZERO;

                BigDecimal safeboxBalance = hasSafebox && safebox.getBalance() != null
                        ? safebox.getBalance()
                        : BigDecimal.ZERO;

                BigDecimal totalAssets = accountBalance.add(safeboxBalance);
                int goalPercent = totalAssets.compareTo(BigDecimal.ZERO) > 0
                        ? safeboxBalance.multiply(BigDecimal.valueOf(100)).divide(totalAssets, 0, BigDecimal.ROUND_DOWN).intValue()
                        : 0;

                // 모델에 글로벌 속성 추가
                model.addAttribute("globalHasAccount", hasAccount);
                model.addAttribute("globalHasSafebox", hasSafebox);
                model.addAttribute("globalAccountNumber", decryptedAccountNumber);
                model.addAttribute("globalAccountBalance", accountBalance);
                model.addAttribute("globalSafeboxBalance", safeboxBalance);
                model.addAttribute("globalTotalAssets", totalAssets);
                model.addAttribute("goalPercent", goalPercent);

            } catch (Exception e) {
                System.out.println("GlobalModelAdvice 오류: " + e.getMessage());
                e.printStackTrace();
                setDefaultGlobalAttributes(model);
            }
        } else {
            setDefaultGlobalAttributes(model);
        }
    }

    private void setDefaultGlobalAttributes(Model model) {
        model.addAttribute("globalHasAccount", false);
        model.addAttribute("globalHasSafebox", false);
        model.addAttribute("globalAccountNumber", null);
        model.addAttribute("globalAccountBalance", BigDecimal.ZERO);
        model.addAttribute("globalSafeboxBalance", BigDecimal.ZERO);
        model.addAttribute("globalTotalAssets", BigDecimal.ZERO);
        model.addAttribute("goalPercent", 0);
    }
}
