// /js/auth.js

// ========================================
// auth.js - 인증 및 UI 처리 (통합 버전)
// ========================================

/**
 * 쿠키에서 특정 이름의 값을 가져옵니다.
 * @param {string} name - 가져올 쿠키의 이름
 * @returns {string|null} 쿠키 값 또는 null
 */
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

/**
 * 특정 이름의 쿠키를 삭제합니다.
 * @param {string} name - 삭제할 쿠키의 이름
 */
function deleteCookie(name) {
    document.cookie = name + '=; Max-Age=0; path=/;';
}

/**
 * Base64로 인코딩된 JWT payload를 디코딩합니다. (UTF-8 지원)
 * @param {string} str - Base64로 인코딩된 문자열
 * @returns {string} 디코딩된 문자열
 */
function b64DecodeUnicode(str) {
    try {
        return decodeURIComponent(atob(str).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
    } catch (e) {
        console.error("Base64 디코딩 실패:", e);
        return null;
    }
}

// --- UI 상태 변경 함수 ---

function showLoggedInState() {
    document.getElementById('logged-in-section').style.display = 'flex'; // 'flex'가 레이아웃에 더 적합할 수 있습니다.
    document.getElementById('logged-out-section').style.display = 'none';
    document.getElementById('service-cards').style.display = 'flex'; // 'flex'로 변경
    document.getElementById('login-notice').style.display = 'none';
}

function showLoggedOutState() {
    document.getElementById('logged-in-section').style.display = 'none';
    document.getElementById('logged-out-section').style.display = 'block';
    document.getElementById('service-cards').style.display = 'none';
    document.getElementById('login-notice').style.display = 'block';
}

/**
 * 페이지의 여러 위치에 사용자 정보를 업데이트합니다.
 * @param {string} name - 사용자 이름
 * @param {string} email - 사용자 이메일
 */
function updateUserInfo(name, email) {
    // 헤더
    document.getElementById('user-name-header').textContent = name + '님';
    // 서비스 카드 (계좌)
    document.getElementById('user-name-display').textContent = name;
    // 서비스 카드 (세이프박스)
    document.getElementById('user-name-display2').textContent = name;
    // 웰컴 모달
    document.getElementById('modal-user-name').textContent = name;

    // 이메일 표시 요소가 있다면 업데이트 (없으면 오류 발생하지 않도록 처리)
    const emailDisplay = document.getElementById('user-email-display');
    if (emailDisplay) {
        emailDisplay.textContent = email;
    }
}

// --- 로그인/로그아웃 함수 ---

function login() {
    window.location.href = '/oauth2/authorization/google';
}

function logout() {
    // 로그아웃 시 서버에 알리는 로직이 있다면 여기에 추가 (예: fetch('/api/logout'))
    deleteCookie('jwt-token');
    window.location.reload(); // 상태를 확실히 초기화하기 위해 페이지를 새로고침합니다.
}

// --- 웰컴 모달 관련 함수 ---

function showWelcomeModal() {
    const modal = document.getElementById('welcome-modal');
    if (modal) {
        modal.style.display = 'block';
    }
}

function closeWelcomeModal() {
    const modal = document.getElementById('welcome-modal');
    if (modal) {
        modal.style.display = 'none';
    }
}


/**
 * 페이지 로드 시 로그인 상태를 확인하고 UI를 업데이트하는 메인 함수
 */
function checkLoginStatus() {
    const token = getCookie('jwt-token');

    if (!token) {
        // 토큰이 없으면 로그아웃 상태 표시
        showLoggedOutState();
        return;
    }

    try {
        const payloadStr = b64DecodeUnicode(token.split('.')[1]);
        const payload = JSON.parse(payloadStr);

        // JWT payload에서 사용자 이름과 이메일 추출
        const userName = payload.username || payload.name || '사용자';
        const userEmail = payload.email || payload.sub || '이메일 정보 없음';

        // UI 업데이트
        updateUserInfo(userName, userEmail);
        showLoggedInState();

        // 계좌 정보 로드
        if (typeof loadAccountInfo === 'function') {
            loadAccountInfo();
        }

        // 로그인 성공 후 웰컴 모달 표시
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.get('login') === 'success') {
            showWelcomeModal();
            // URL 파라미터 제거
            window.history.replaceState({}, document.title, window.location.pathname);
        }

    } catch (e) {
        console.error('JWT 토큰 처리 중 오류:', e);
        // 토큰 문제 시 로그아웃 상태 표시
        showLoggedOutState();
    }
}


// --- 이벤트 리스너 ---

// 페이지 로드가 완료되면 로그인 상태 확인 실행
document.addEventListener('DOMContentLoaded', checkLoginStatus);