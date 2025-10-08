(() => {
    const accountModal = document.getElementById('accountModal');
    const openModalBtn = document.getElementById('openModalBtn');
    const closeModalBtn = document.querySelector('#accountModal .close');
    const confirmAccountBtn = document.getElementById('confirmAccountBtn');
    const accountInputModal = document.getElementById('accountInputModal');

    const accountDisplay = document.getElementById('accountDisplay');
    const transferAmountInput = document.getElementById('transferAmount');
    const accountPasswordInput = document.getElementById('accountPassword');
    const transferMemoInput = document.getElementById('transferMemo');
    const transferBtn = document.getElementById('transferBtn');

    const confirmTransferModal = document.getElementById('confirmTransferModal');
    const closeConfirmModalBtn = confirmTransferModal.querySelector('.close');
    const confirmAccount = document.getElementById('confirmAccount');
    const confirmRecipient = document.getElementById('confirmRecipient');
    const confirmAmount = document.getElementById('confirmAmount');
    const confirmMemo = document.getElementById('confirmMemo');
    const finalTransferBtn = document.getElementById('finalTransferBtn');
    const cancelTransferBtn = document.getElementById('cancelTransferBtn');

    const keypad = document.getElementById('keypad');
    const keypadDisplay = document.getElementById('keypadDisplay');
    const keypadNumberBtns = document.querySelectorAll('.keypad-btn[data-key]');
    const keypadClearBtn = document.getElementById('keypadClearBtn');
    const keypadClearAllBtn = document.getElementById('keypadClearAllBtn');
    const keypadCancelBtn = document.getElementById('keypadCancelBtn');
    const keypadConfirmBtn = document.getElementById('keypadConfirmBtn');

    let selectedAccountInfo = null;

    // ------------------- 전역 함수 -------------------
    window.formatAmount = (input) => {
        let value = input.value.replace(/[^0-9]/g, '');
        value = value.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        input.value = value;
        checkTransferButton();
    };

    window.showKeypad = () => {
        keypad.style.display = 'flex';
        keypadDisplay.value = '';
    };

    const hideKeypad = () => { keypad.style.display = 'none'; };
    const inputNumber = (num) => { if (keypadDisplay.value.length < 4) keypadDisplay.value += num; };
    const clearInput = () => { keypadDisplay.value = keypadDisplay.value.slice(0, -1); };
    const clearAll = () => { keypadDisplay.value = ''; };
    const confirmKeypad = () => {
        if (keypadDisplay.value.length === 4) {
            accountPasswordInput.value = keypadDisplay.value;
            hideKeypad();
            checkTransferButton();
        } else { alert('4자리 비밀번호를 입력해주세요.'); }
    };

    const checkTransferButton = () => {
        const amount = parseInt(transferAmountInput.value.replace(/,/g, ''), 10);
        const hasAccount = selectedAccountInfo !== null;
        const hasPassword = accountPasswordInput.value.trim().length === 4;
        transferBtn.disabled = !hasAccount || isNaN(amount) || amount <= 0 || !hasPassword;
    };

    const checkAccount = async () => {
        const accountNumber = accountInputModal.value.trim().replace(/[^0-9]/g, '');
        if (accountNumber.length === 0) { alert("계좌번호를 입력해주세요."); return; }
        
        // 메타 태그에서 이메일 해시 읽기
        const emailHashMeta = document.querySelector('meta[name="user-email-hash"]');
        const emailHash = emailHashMeta ? emailHashMeta.content : null;

        if (!emailHash) {
            alert("인증 정보가 없습니다. 새로고침 후 다시 시도해주세요.");
            return;
        }
        
        try {
            confirmAccountBtn.disabled = true;
            confirmAccountBtn.textContent = "조회중...";
            const response = await fetch('/banking/check-account', {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'x-user-email-hash': emailHash // 헤더 추가
                },
                body: `accountNumber=${accountNumber}`
            });
            const result = await response.json();
            if (result.success) {
                selectedAccountInfo = result.accountInfo;
                const formatted = accountNumber.replace(/^(\d{3})(\d{4})(\d{5})$/, '$1-$2-$3');
                const userName = selectedAccountInfo.USER_NAME || '알 수 없음';
                openModalBtn.style.display = 'none';
                accountDisplay.textContent = `${formatted} (${userName})`;
                accountDisplay.style.display = 'inline';
                accountModal.style.display = 'none';
                checkTransferButton();
            } else { alert(result.message || '계좌 조회에 실패했습니다.'); }
        } catch (error) { alert('계좌 조회 중 오류가 발생했습니다.'); console.error('Error:', error); }
        finally { confirmAccountBtn.disabled = false; confirmAccountBtn.textContent = "확인"; }
    };

    // ------------------- 이벤트 리스너 바로 등록 -------------------
    openModalBtn.addEventListener('click', () => { 
        console.log('계좌번호 입력 버튼 클릭됨!'); // 디버깅용
        accountModal.style.display = 'flex'; 
        accountInputModal.value = ''; 
        accountInputModal.focus(); 
    });
    
    closeModalBtn.addEventListener('click', () => { accountModal.style.display = 'none'; });
    confirmAccountBtn.addEventListener('click', checkAccount);
    
    accountDisplay.addEventListener('click', () => {
        accountDisplay.style.display = 'none';
        openModalBtn.style.display = 'inline';
        selectedAccountInfo = null;
        accountModal.style.display = 'flex';
        accountInputModal.value = '';
        accountInputModal.focus();
        checkTransferButton();
    });

    transferAmountInput.addEventListener('input', (e) => formatAmount(e.target));
    accountPasswordInput.addEventListener('click', showKeypad);

    keypadNumberBtns.forEach(btn => btn.addEventListener('click', () => inputNumber(btn.dataset.key)));
    keypadClearBtn.addEventListener('click', clearInput);
    keypadClearAllBtn.addEventListener('click', clearAll);
    keypadCancelBtn.addEventListener('click', hideKeypad);
    keypadConfirmBtn.addEventListener('click', confirmKeypad);

    transferBtn.addEventListener('click', () => {
        confirmAccount.textContent = `계좌번호: ${accountDisplay.textContent || "입력된 계좌 없음"}`;
        confirmRecipient.textContent = `받는 분: ${selectedAccountInfo?.USER_NAME || '알 수 없음'}`;
        confirmAmount.textContent = `이체 금액: ${transferAmountInput.value || "0"} 원`;
        confirmMemo.textContent = `메모: ${transferMemoInput.value || "메모 없음"}`;
        confirmTransferModal.style.display = 'flex';
    });

    closeConfirmModalBtn.addEventListener('click', () => confirmTransferModal.style.display = 'none');
    cancelTransferBtn.addEventListener('click', () => confirmTransferModal.style.display = 'none');

    // 수정된 이체 처리 - transfer_3 페이지로 이동
    finalTransferBtn.onclick = null; // 기존 이벤트 제거
    finalTransferBtn.addEventListener('click', async () => {
        console.log('=== 이체 버튼 클릭됨 ===');
        
        const getCookie = (name) => {
            const value = `; ${document.cookie}`;
            const parts = value.split(`; ${name}=`);
            if (parts.length === 2) return parts.pop().split(';').shift();
        };
        
        const token = getCookie('jwt-token');
        const emailHashMeta = document.querySelector('meta[name="user-email-hash"]');
        const emailHash = emailHashMeta ? emailHashMeta.content : null;
        
        console.log('토큰 존재:', !!token);
        console.log('이메일 해시 존재:', !!emailHash);
        
        if (!token) {
            alert('로그인 토큰이 없습니다. 다시 로그인해주세요.');
            return;
        }
        
        if (!emailHash) {
            alert('인증 정보가 없습니다. 페이지를 새로고침해주세요.');
            return;
        }
        
        try {
            finalTransferBtn.disabled = true;
            finalTransferBtn.textContent = "처리중...";
            
            const response = await fetch('/banking/transfer', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'x-user-email-hash': emailHash
                },
                body: `recipientName=${encodeURIComponent(selectedAccountInfo?.USER_NAME || "")}&toAccountNumber=${selectedAccountInfo?.ACCOUNT_NUMBER || ""}&amount=${parseInt(transferAmountInput.value.replace(/,/g, ""), 10)}&memo=${encodeURIComponent(transferMemoInput.value.trim())}&password=${accountPasswordInput.value}`
            });
            
            console.log('응답 상태:', response.status);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
            
            const result = await response.json();
            console.log('응답 결과:', result);
            
            if (result.success) {
                // 성공 시 transfer_3 페이지로 이동
                confirmTransferModal.style.display = 'none';
                
                // URL 파라미터 구성
                const params = new URLSearchParams({
                    recipientName: result.recipientName || selectedAccountInfo?.USER_NAME || "",
                    toAccountNumber: result.toAccountNumber || selectedAccountInfo?.ACCOUNT_NUMBER || "",
                    amount: result.amount || parseInt(transferAmountInput.value.replace(/,/g, ""), 10),
                    memo: result.memo || transferMemoInput.value.trim() || ""
                });
                
                // transfer_3 페이지로 이동
                window.location.href = '/banking/transfer_3?' + params.toString();
                
            } else {
                alert(result.message || '이체에 실패했습니다.');
            }
            
        } catch (error) {
            console.error('이체 에러:', error);
            alert('이체 처리 중 오류가 발생했습니다: ' + error.message);
        } finally {
            finalTransferBtn.disabled = false;
            finalTransferBtn.textContent = "이체하기";
        }
    });

    window.addEventListener('click', e => {
        if (e.target === accountModal) accountModal.style.display = 'none';
        if (e.target === confirmTransferModal) confirmTransferModal.style.display = 'none';
    });

    console.log('FINAL 스크립트 로드 완료 - ' + new Date().getTime());
})();