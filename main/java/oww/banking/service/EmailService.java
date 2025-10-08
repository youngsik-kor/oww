package oww.banking.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${mail.from.email}")
    private String fromEmail;

    @Value("${mail.from.name}")
    private String fromName;

    /**
     * 이메일 인증번호 발송
     * @param toEmail 받는 이메일
     * @param verificationCode 인증번호
     * @param userName 사용자 이름 (선택)
     */
    public void sendVerificationEmail(String toEmail, String verificationCode, String userName) {
        System.out.println("[MAIL] sendVerificationEmail() 시작 → to=" + toEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("[Own Wedding Wallet] 계좌 개설 이메일 인증");

            String htmlContent = createVerificationEmailContent(verificationCode, userName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("[MAIL] 발송 성공 → to=" + toEmail);

        } catch (Exception e) {
            System.out.println("[MAIL] 발송 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }


    /**
     * 인증 이메일 HTML 템플릿 생성
     */
    private String createVerificationEmailContent(String verificationCode, String userName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>이메일 인증</title>
                <style>
                    body { font-family: 'Noto Sans KR', Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; }
                    .header { text-align: center; background-color: #4CAF50; color: white; padding: 20px; border-radius: 10px 10px 0 0; }
                    .content { background-color: white; padding: 30px; border-radius: 0 0 10px 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .verification-code { background-color: #f0f8ff; border:2px dashed #4CAF50; padding: 20px; text-align: center; margin: 20px 0; border-radius: 8px; }
                    .code { font-size: 32px; font-weight: bold; color: #4CAF50; letter-spacing: 8px; }
                    .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🏦 Own Wedding Wallet</h1>
                        <h2>계좌 개설 이메일 인증</h2>
                    </div>
                    <div class="content">
                        <h3>안녕하세요%s!</h3>
                        <p>Own Wedding Wallet 계좌 개설을 위한 이메일 인증번호를 발송해드립니다.</p>
                        
                        <div class="verification-code">
                            <p><strong>인증번호</strong></p>
                            <div class="code">%s</div>
                        </div>
                        
                        <p>위 인증번호를 계좌 개설 페이지에 입력해주세요.</p>
                        
                        <div class="warning">
                            <strong>⚠️ 주의사항</strong>
                            <ul>
                                <li>인증번호는 <strong>10분간</strong> 유효합니다.</li>
                                <li>본인이 요청하지 않은 경우, 이 이메일을 무시하세요.</li>
                                <li>인증번호를 타인에게 공유하지 마세요.</li>
                            </ul>
                        </div>
                        
                        <p>감사합니다.<br>Own Wedding Wallet 팀</p>
                    </div>
                    <div class="footer">
                        <p>이 이메일은 자동으로 발송된 메일입니다.<br>
                        © 2025 Own Wedding Wallet. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            userName != null ? " " + userName + "님" : "", 
            verificationCode
        );
    }

    /**
     * 간단한 텍스트 이메일 발송 (백업용)
     */
    public void sendSimpleVerificationEmail(String toEmail, String verificationCode) {
        try {
            System.out.println("📧 간단한 텍스트 이메일 발송 시작: " + toEmail);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("[Own Wedding Wallet] 계좌 개설 인증번호");
            
            String content = String.format(
                "안녕하세요!\n\n" +
                "Own Wedding Wallet 계좌 개설을 위한 인증번호입니다.\n\n" +
                "인증번호: %s\n\n" +
                "이 인증번호는 10분간 유효합니다.\n" +
                "본인이 요청하지 않은 경우, 이 이메일을 무시하세요.\n\n" +
                "감사합니다.\n" +
                "Own Wedding Wallet 팀",
                verificationCode
            );
            
            helper.setText(content, false);
            mailSender.send(message);
            
            System.out.println("✅ 간단 이메일 발송 성공: " + toEmail);

        } catch (Exception e) {
            System.out.println("❌ 간단 이메일 발송 실패: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage());
        }
    }
}