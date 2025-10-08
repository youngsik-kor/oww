/**
 * ========================================
 * 계좌 개설 페이지 전용 JavaScript
 * ========================================
 */
document.addEventListener('DOMContentLoaded', () => {

    // ========================================
    // DOM 요소 가져오기
    // ========================================
    const form = document.querySelector('form[action="/banking/account/create"]');
    const nameInput = document.getElementById('name');
    const emailInput = document.getElementById('email');
    const sendEmailBtn = document.querySelector('button[onclick^="sendEmailVerification"]');
    
    const emailVerifySection = document.getElementById('emailVerifySection');
    const emailCodeInput = document.getElementById('emailCode');
    const verifyEmailBtn = document.querySelector('button[onclick^="verifyEmailCode"]');

    const passwordDisplay = document.getElementById('passwordDisplay');
    const passwordInput = document.getElementById('passwordInput');
    const keypad = document.getElementById('keypad');
    const keypadButtons = document.querySelectorAll('.keypad button');

    const agreeCheckbox = document.getElementById('agree');
    const termsLink = document.querySelector('.terms-link');
    const termsModal = document.getElementById('termsModal');
    const closeModalBtn = document.querySelector('.modal .close');

    // ========================================
    // 함수 정의
    // ========================================

    // 키패드 보이기/숨기기
    const showKeypad = () => keypad.style.display = "grid";
    const hideKeypad = () => keypad.style.display = "none";

    // 비밀번호 입력 값에 따라 화면 업데이트 (● ● ○ ○)
    const updateDisplay = () => {
        const length = passwordInput.value.length;
        let display = "";
        for (let i = 0; i < 4; i++) {
            display += i < length ? "● " : "○ ";
        }
        passwordDisplay.textContent = display.trim();
    };

    // 키패드 숫자 추가
    const addNumber = (num) => {
        if (passwordInput.value.length < 4) {
            passwordInput.value += num;
            updateDisplay();
        }
    };

    // 키패드 숫자 삭제
    const removeNumber = () => {
        passwordInput.value = passwordInput.value.slice(0, -1);
        updateDisplay();
    };
    
    // 약관 모달 열기/닫기
    const openModal = (e) => {
        e.preventDefault();
        termsModal.style.display = "flex";
    };
    const closeModal = () => termsModal.style.display = "none";

    // 이메일 인증번호 발송
    const sendEmailVerification = async (event) => {
        const button = event.target;
        const email = emailInput.value;

        if (!email || !email.includes("@")) {
            alert("유효한 이메일 주소를 입력하세요.");
            return;
        }

        const originalText = button.textContent;
        button.textContent = "발송 중...";
        button.disabled = true;

        try {
            const response = await fetch('/banking/account/send-verification', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: 'email=' + encodeURIComponent(email)
            });
            const data = await response.json();
            
			if (data.success) {
			    alert("✅ 인증 메일이 발송되었습니다."); 
			    emailVerifySection.style.display = "block";
			} else {
			    alert("❌ " + data.message);
            }
        } catch (err) {
            console.error("이메일 발송 오류:", err);
            alert("인증번호 발송 중 오류가 발생했습니다.");
        } finally {
            button.textContent = originalText;
            button.disabled = false;
        }
    };

    // 이메일 인증번호 확인
    const verifyEmailCode = async (event) => {
        const button = event.target;
        const email = emailInput.value;
        const code = emailCodeInput.value;

        if (!code || code.length !== 6) {
            alert("6자리 인증번호를 입력하세요.");
            return;
        }
        
        const originalText = button.textContent;
        button.textContent = "확인 중...";
        button.disabled = true;

        try {
            const response = await fetch('/banking/account/verify-email', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: `email=${encodeURIComponent(email)}&code=${encodeURIComponent(code)}`
            });
            const data = await response.json();

            if (data.success) {
                alert("✅ " + data.message);
                emailCodeInput.style.backgroundColor = "#d4edda";
                emailCodeInput.readOnly = true;
                button.textContent = "✅ 인증 완료";
                button.style.backgroundColor = "#28a745";
                button.disabled = true;
            } else {
                alert("❌ " + data.message);
                button.textContent = originalText;
                button.disabled = false;
            }
        } catch (err) {
            console.error("인증 확인 오류:", err);
            alert("인증 확인 중 오류가 발생했습니다.");
            button.textContent = originalText;
            button.disabled = false;
        }
    };

    // 최종 폼 제출
    const submitForm = async (event) => {
        event.preventDefault(); // 폼의 기본 제출 동작을 막음

        // 유효성 검사
        if (!nameInput.value.trim()) {
            alert("이름을 입력해주세요.");
            return;
        }
        if (!emailInput.value || !emailInput.value.includes("@")) {
            alert("유효한 이메일을 입력해주세요.");
            return;
        }
        if (!emailCodeInput.value || emailCodeInput.value.length !== 6) {
            alert("이메일 인증번호 6자리를 입력해주세요.");
            return;
        }
        if (passwordInput.value.length !== 4) {
            alert("비밀번호 4자리를 입력해주세요.");
            showKeypad();
            passwordDisplay.focus();
            return;
        }
        if (!agreeCheckbox.checked) {
            alert("약관에 동의해야 계좌를 개설할 수 있습니다.");
            return;
        }

        const submitBtn = form.querySelector('.submit-btn');
        submitBtn.textContent = "계좌 생성 중...";
        submitBtn.disabled = true;
        
        const formData = new URLSearchParams({
            name: nameInput.value,
            email: emailInput.value,
            password: passwordInput.value,
            emailCode: emailCodeInput.value
        }).toString();

        try {
            const response = await fetch('/banking/account/create', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: formData
            });
            const data = await response.json();

            if (data.success) {
                window.location.href = '/banking/account/success';
            } else {
                alert("계좌 생성 실패: " + data.message);
                submitBtn.textContent = "계좌 개설하기";
                submitBtn.disabled = false;
            }
        } catch (err) {
            console.error("계좌 생성 오류:", err);
            alert("계좌 생성 중 오류가 발생했습니다.");
            submitBtn.textContent = "계좌 개설하기";
            submitBtn.disabled = false;
        }
    };

    // ========================================
    // 이벤트 리스너 할당
    // ========================================
    
    // 폼 제출 이벤트
    form.addEventListener('submit', submitForm);
    
    // 이메일 인증 버튼
    sendEmailBtn.addEventListener('click', sendEmailVerification);
    verifyEmailBtn.addEventListener('click', verifyEmailCode);

    // 비밀번호 키패드 관련
    passwordDisplay.addEventListener('click', showKeypad);
    keypadButtons.forEach(button => {
        button.addEventListener('click', () => {
            const value = button.textContent;
            if (value >= '0' && value <= '9') {
                addNumber(value);
            } else if (value === '삭제') {
                removeNumber();
            } else if (value === '취소') {
                hideKeypad();
            }
        });
    });

    // 약관 모달 관련
    termsLink.addEventListener('click', openModal);
    closeModalBtn.addEventListener('click', closeModal);
    // 모달 바깥 영역 클릭 시 닫기
    termsModal.addEventListener('click', (event) => {
        if (event.target === termsModal) {
            closeModal();
        }
    });

    // ========================================
    // 초기화 함수 실행
    // ========================================
    updateDisplay(); // 페이지 로드 시 비밀번호 디스플레이 초기화
});