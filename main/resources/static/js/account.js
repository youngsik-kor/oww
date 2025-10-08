// ========================================
// 계좌 관련 함수들 (완전한 토큰 기반)
// ========================================

/**
 * JWT 토큰을 헤더에 포함한 Fetch 요청
 */
function fetchWithToken(url, options = {}) {
    const token = getCookie('jwt-token');
    
    const defaultOptions = {
        credentials: 'include',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            ...options.headers
        }
    };
    
    // JWT 토큰이 있으면 Authorization 헤더에 추가
    if (token) {
        defaultOptions.headers['Authorization'] = `Bearer ${token}`;
    }
    
    return fetch(url, { ...defaultOptions, ...options });
}

/**
 * 계좌 정보 로드 (완전한 토큰 기반)
 */
function loadAccountInfo() {
   const url = '/api/account';
    console.log('토큰 기반 API 호출:', url);
    
    // 토큰 확인
    const token = getCookie('jwt-token');
    if (!token) {
        console.error('JWT 토큰이 없습니다.');
        showLoggedOutState();
        return;
    }
    
    fetchWithToken(url, {
        method: 'GET'
    })
    .then(res => {
        console.log('응답 상태:', res.status);
        
        if (res.status === 401) {
            // 토큰이 만료되었거나 유효하지 않음
            console.log('토큰 인증 실패 - 로그아웃 처리');
            deleteCookie('jwt-token');
            showLoggedOutState();
            return null;
        }
        
        if (!res.ok) {
            throw new Error(`HTTP error! status: ${res.status}`);
        }
        
        return res.json();
    })
    .then(data => {
        if (!data) return; // 401 에러로 인한 null 처리
        
        console.log('토큰 기반 API 응답:', data);
        
        if (data.success) {
            updateAccountDisplay(data);
            updateSafeboxDisplay(data);
            updateUserDisplay(data);
            
            // 서비스 카드 표시
            const serviceCards = document.getElementById('service-cards');
            if (serviceCards) {
                serviceCards.style.display = 'block';
            }
        } else {
            console.error('API 응답 실패:', data.message);
            
            // 인증 관련 오류면 로그아웃 처리
            if (data.message && data.message.includes('토큰')) {
                deleteCookie('jwt-token');
                showLoggedOutState();
            } else {
                showError('계좌 정보를 불러오는데 실패했습니다: ' + (data.message || '알 수 없는 오류'));
            }
        }
    })
    .catch(err => {
        console.error('계좌 정보 로드 실패:', err);
        showError('서버와 연결할 수 없습니다. 잠시 후 다시 시도해주세요.');
    });
}

/**
 * 이메일 인증번호 발송 (토큰 기반)
 */
function sendEmailVerification(email) {
    return fetchWithToken('/account/send-verification', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: `email=${encodeURIComponent(email)}`
    })
    .then(res => {
        if (res.status === 401) {
            throw new Error('로그인이 필요합니다.');
        }
        return res.json();
    })
    .then(data => {
        if (data.success) {
            return { success: true, message: data.message };
        } else {
            throw new Error(data.message || '인증번호 발송에 실패했습니다.');
        }
    })
    .catch(err => {
        console.error('인증번호 발송 실패:', err);
        return { success: false, message: err.message };
    });
}

/**
 * 이메일 인증번호 확인 (토큰 기반)
 */
function verifyEmailCode(email, code) {
    return fetchWithToken('/account/verify-email', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: `email=${encodeURIComponent(email)}&code=${encodeURIComponent(code)}`
    })
    .then(res => {
        if (res.status === 401) {
            throw new Error('로그인이 필요합니다.');
        }
        return res.json();
    })
    .then(data => {
        if (data.success) {
            return { success: true, message: data.message };
        } else {
            throw new Error(data.message || '인증번호 확인에 실패했습니다.');
        }
    })
    .catch(err => {
        console.error('인증번호 확인 실패:', err);
        return { success: false, message: err.message };
    });
}

/**
 * 계좌 정보 표시 업데이트
 */
function updateAccountDisplay(data) {
    const accountCard = document.querySelector('.service-card:first-child');
    if (!accountCard) return;
    
    if (data.hasAccount) {
        // 계좌 있음 - 실제 데이터 표시
        const balanceElement = accountCard.querySelector('.balance-amount');
        const accountNumberElement = accountCard.querySelector('h2');
        
        if (balanceElement) {
            balanceElement.textContent = `₩${data.balance.toLocaleString()}`;
        }
        if (accountNumberElement) {
            accountNumberElement.textContent = data.accountNumber;
        }
    } else {
        // 계좌 없음 - 생성 유도
        accountCard.innerHTML = `
            <div style="text-align: center; padding: 30px; width: 100%;">
                <h2>📭 계좌가 없습니다</h2>
                <p style="margin: 15px 0; color: #666;">
                    Own Wedding Wallet 계좌를 생성해주세요
                </p>
                <button onclick="goToCreateAccount()"
                        class="create-account-btn">
                    계좌 생성하기
                </button>
            </div>
        `;
    }
}

/**
 * 세이프박스 정보 표시 업데이트
 */
function updateSafeboxDisplay(data) {
    const safeboxCard = document.querySelector('.service-card:nth-child(2)');
    if (!safeboxCard) return;
    
    if (data.hasSafebox) {
        const balanceElement = safeboxCard.querySelector('.balance-amount');
        if (balanceElement) {
            balanceElement.textContent = `₩${data.safeboxBalance.toLocaleString()}`;
        }
    } else {
        safeboxCard.innerHTML = `
            <div style="text-align: center; padding: 30px; width: 100%;">
                <h2>🔒 세이프박스가 없습니다</h2>
                <p style="margin: 15px 0; color: #666;">
                    세이프박스를 생성하여 안전하게 자금을 보관하세요
                </p>
                <button onclick="goToCreateSafebox()"
                        class="create-safebox-btn">
                    세이프박스 생성하기
                </button>
            </div>
        `;
    }
}

/**
 * 사용자 이름 표시 업데이트
 */
function updateUserDisplay(data) {
    if (!data.userName) return;
    
    // 안전하게 DOM 요소 업데이트
    const userDisplayElements = [
        document.getElementById('user-name-display'),
        document.getElementById('user-name-display2')
    ];
    
    userDisplayElements.forEach(element => {
        if (element) {
            element.textContent = data.userName;
        }
    });
}

/**
 * 오류 메시지 표시
 */
function showError(message) {
    // 간단한 오류 표시 (나중에 toast나 modal로 개선 가능)
    alert(message);
}

/**
 * 계좌 생성 페이지로 이동
 */
function goToCreateAccount() {
    // 토큰 확인 후 이동
    const token = getCookie('jwt-token');
    if (!token) {
        alert('로그인이 필요합니다.');
        login();
        return;
    }
    
    window.location.href = '/banking/createAccount';
}

/**
 * 세이프박스 생성 페이지로 이동
 */
function goToCreateSafebox() {
    // 토큰 확인 후 이동
    const token = getCookie('jwt-token');
    if (!token) {
        alert('로그인이 필요합니다.');
        login();
        return;
    }
    
    window.location.href = '/banking/safebox';
}

/**
 * Banking 서비스 버튼 이벤트 설정 (토큰 기반)
 */
function setupBankingButtons() {
    const bankingButtons = document.querySelectorAll('.banking-btn');
    
    bankingButtons.forEach(btn => {
        // 기존 이벤트 리스너 제거
        btn.removeEventListener('click', handleBankingButtonClick);
        // 새 이벤트 리스너 추가
        btn.addEventListener('click', handleBankingButtonClick);
    });
}

/**
 * Banking 버튼 클릭 핸들러 (토큰 기반)
 */
function handleBankingButtonClick(e) {
    e.preventDefault();
    
    const btn = e.currentTarget;
    const redirect = btn.dataset.redirect;
    
    // 토큰 확인
    const token = getCookie('jwt-token');
    if (!token) {
        alert('로그인이 필요합니다.');
        login();
        return;
    }
    
    if (!redirect) {
        console.error('버튼에 redirect 경로가 없습니다:', btn);
        return;
    }
    
    // 토큰이 있으면 바로 이동 (서버에서 토큰 검증)
    window.location.href = redirect;
}

// auth.js에서 사용할 함수들 (전역 함수로 노출)
window.loadAccountInfo = loadAccountInfo;
window.sendEmailVerification = sendEmailVerification;
window.verifyEmailCode = verifyEmailCode;

// DOM이 로드된 후 Banking 버튼 설정
document.addEventListener('DOMContentLoaded', function() {
    setupBankingButtons();
});