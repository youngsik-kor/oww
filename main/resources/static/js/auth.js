// ========================================
// auth.js - 디버깅 강화 버전
// ========================================

/**
 * 쿠키에서 특정 이름의 값을 가져옵니다.
 */
function getCookie(name) {
    console.log('getCookie 호출:', name);
    console.log('전체 쿠키:', document.cookie);
    
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    const result = parts.length === 2 ? parts.pop().split(';').shift() : null;
    
    console.log(`쿠키 ${name} 결과:`, result);
    return result;
}

/**
 * JWT 토큰의 유효성을 검사합니다.
 */
function validateJwtToken(token) {
    console.log('JWT 토큰 검증 시작:', token ? '토큰 있음' : '토큰 없음');
    
    if (!token) {
        console.log('토큰이 없습니다.');
        return null;
    }
    
    try {
        const parts = token.split('.');
        console.log('JWT 파트 개수:', parts.length);
        
        if (parts.length !== 3) {
            console.error("JWT 토큰 형식 오류 - 파트 개수:", parts.length);
            return null;
        }
        
        // Base64 디코딩 시도
        console.log('JWT Payload 파트:', parts[1]);
        
        const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
        const payloadStr = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        
        console.log('디코딩된 Payload 문자열:', payloadStr);
        
        const payload = JSON.parse(payloadStr);
        console.log('파싱된 JWT Payload:', payload);
        
        // 토큰 만료 시간 확인
        if (payload.exp) {
            const expDate = new Date(payload.exp * 1000);
            const now = new Date();
            console.log('토큰 만료 시간:', expDate);
            console.log('현재 시간:', now);
            console.log('토큰 유효함:', expDate > now);
            
            if (payload.exp < Date.now() / 1000) {
                console.log("JWT 토큰이 만료되었습니다.");
                return null;
            }
        }
        
        return payload;
    } catch (e) {
        console.error('JWT 토큰 검증 실패:', e);
        return null;
    }
}

/**
 * 사용자 정보를 추출합니다.
 */
function extractUserInfo(payload) {
    console.log('사용자 정보 추출 시작:', payload);
    
    // 다양한 필드에서 사용자 이름 추출 시도
    const possibleNameFields = ['name', 'username', 'given_name', 'family_name', 'sub'];
    const possibleEmailFields = ['email', 'sub', 'preferred_username'];
    
    let userName = null;
    let userEmail = null;
    
    // 이름 찾기
    for (const field of possibleNameFields) {
        if (payload[field]) {
            userName = payload[field];
            console.log(`사용자 이름 발견 (${field}):`, userName);
            break;
        }
    }
    
    // 이메일 찾기
    for (const field of possibleEmailFields) {
        if (payload[field]) {
            userEmail = payload[field];
            console.log(`사용자 이메일 발견 (${field}):`, userEmail);
            break;
        }
    }
    
    if (!userName) {
        console.warn('사용자 이름을 찾을 수 없습니다. 기본값 사용');
        userName = '사용자';
    }
    
    if (!userEmail) {
        console.warn('사용자 이메일을 찾을 수 없습니다. 기본값 사용');
        userEmail = '이메일 정보 없음';
    }
    
    console.log('최종 사용자 정보:', { name: userName, email: userEmail });
    return { name: userName, email: userEmail };
}

/**
 * UI 상태 변경 함수들
 */
function showLoggedInState() {
    console.log('로그인 상태 UI 표시 시작');
    
    const elements = {
        loggedInSection: document.getElementById('logged-in-section'),
        loggedOutSection: document.getElementById('logged-out-section'),
        serviceCards: document.getElementById('service-cards'),
        loginNotice: document.getElementById('login-notice')
    };
    
    console.log('UI 요소들:', elements);
    
    if (elements.loggedInSection) {
        elements.loggedInSection.style.display = 'flex';
        console.log('logged-in-section 표시');
    } else {
        console.error('logged-in-section 요소를 찾을 수 없습니다!');
    }
    
    if (elements.loggedOutSection) {
        elements.loggedOutSection.style.display = 'none';
        console.log('logged-out-section 숨김');
    } else {
        console.error('logged-out-section 요소를 찾을 수 없습니다!');
    }
    
    if (elements.serviceCards) {
        elements.serviceCards.style.display = 'flex';
        console.log('service-cards 표시');
    } else {
        console.error('service-cards 요소를 찾을 수 없습니다!');
    }
    
    if (elements.loginNotice) {
        elements.loginNotice.style.display = 'none';
        console.log('login-notice 숨김');
    } else {
        console.error('login-notice 요소를 찾을 수 없습니다!');
    }
}

function showLoggedOutState() {
    console.log('로그아웃 상태 UI 표시 시작');
    
    const elements = {
        loggedInSection: document.getElementById('logged-in-section'),
        loggedOutSection: document.getElementById('logged-out-section'),
        serviceCards: document.getElementById('service-cards'),
        loginNotice: document.getElementById('login-notice')
    };
    
    if (elements.loggedInSection) elements.loggedInSection.style.display = 'none';
    if (elements.loggedOutSection) elements.loggedOutSection.style.display = 'block';
    if (elements.serviceCards) elements.serviceCards.style.display = 'none';
    if (elements.loginNotice) elements.loginNotice.style.display = 'block';
    
    console.log('로그아웃 UI 표시 완료');
}

/**
 * 사용자 정보 UI 업데이트
 */
function updateUserInfo(name, email) {
    console.log('사용자 정보 UI 업데이트:', name, email);
    
    const userElements = {
        userNameHeader: document.getElementById('user-name-header'),
        userNameDisplay: document.getElementById('user-name-display'),
        userNameDisplay2: document.getElementById('user-name-display2'),
        modalUserName: document.getElementById('modal-user-name')
    };
    
    console.log('사용자 이름 표시 요소들:', userElements);
    
    Object.entries(userElements).forEach(([key, element]) => {
        if (element) {
            const displayText = key === 'userNameHeader' ? name + '님' : name;
            element.textContent = displayText;
            console.log(`${key} 업데이트 완료:`, displayText);
        } else {
            console.warn(`${key} 요소를 찾을 수 없습니다.`);
        }
    });
}

/**
 * 메인 로그인 상태 확인 함수
 */
function checkLoginStatus() {
    console.log('=== 로그인 상태 확인 시작 ===');
    
    const token = getCookie('jwt-token');
    
    if (!token) {
        console.log('JWT 토큰이 없습니다 - 로그아웃 상태 표시');
        showLoggedOutState();
        return false;
    }
    
    console.log('JWT 토큰 발견, 검증 시작...');
    
    // JWT 토큰 검증
    const payload = validateJwtToken(token);
    
    if (!payload) {
        console.log('JWT 토큰이 유효하지 않습니다 - 로그아웃 상태 표시');
        showLoggedOutState();
        return false;
    }
    
    console.log('JWT 토큰 검증 성공');
    
    // 사용자 정보 추출
    const userInfo = extractUserInfo(payload);
    
    // UI 업데이트
    console.log('UI 업데이트 시작...');
    updateUserInfo(userInfo.name, userInfo.email);
    showLoggedInState();
    
    console.log('=== 로그인 상태 확인 완료 ===');
    return true;
}

/**
 * 로그인 함수
 */
function login() {
    console.log('로그인 시작...');
    const authUrl = '/oauth2/authorization/google';
    console.log('리다이렉트 URL:', authUrl);
    window.location.href = authUrl;
}

/**
 * 로그아웃 함수
 */
function logout() {
    console.log('로그아웃 시작...');
    document.cookie = 'jwt-token=; Max-Age=0; path=/;';
    document.cookie = 'refresh-token=; Max-Age=0; path=/;';
    window.location.href = '/';
}

/**
 * 웰컴 모달 함수들
 */
function showWelcomeModal() {
    console.log('웰컴 모달 표시');
    const modal = document.getElementById('welcome-modal');
    if (modal) modal.style.display = 'block';
}

function closeWelcomeModal() {
    console.log('웰컴 모달 닫기');
    const modal = document.getElementById('welcome-modal');
    if (modal) modal.style.display = 'none';
}

/**
 * URL 파라미터 확인
 */
function checkLoginSuccess() {
    const urlParams = new URLSearchParams(window.location.search);
    const loginStatus = urlParams.get('login');
    
    console.log('URL 파라미터 login:', loginStatus);
    
    if (loginStatus === 'success') {
        console.log('로그인 성공 감지 - 웰컴 모달 표시');
        showWelcomeModal();
        
        // URL 파라미터 제거
        const url = new URL(window.location);
        url.searchParams.delete('login');
        window.history.replaceState({}, document.title, url);
    }
}

/**
 * 페이지 로드 완료 시 실행
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM 로드 완료 - 디버깅 모드로 인증 시스템 초기화');
    
    // URL 파라미터 확인
    checkLoginSuccess();
    
    // 로그인 상태 확인
    checkLoginStatus();
    
    console.log('초기화 완료');
});