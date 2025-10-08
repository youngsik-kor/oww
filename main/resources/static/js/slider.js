///// Slider Auto Switching (1세트에서 개선) /////

// jQuery 없이 순수 JavaScript로 구현
function bannerSwitcher() {
    const currentInput = document.querySelector('.sec-1-input:checked');
    let nextInput = currentInput.nextElementSibling;
    
    // 클래스가 sec-1-input인 다음 요소 찾기
    while (nextInput && !nextInput.classList.contains('sec-1-input')) {
        nextInput = nextInput.nextElementSibling;
    }
    
    if (nextInput && nextInput.classList.contains('sec-1-input')) {
        nextInput.checked = true;
    } else {
        // 마지막이면 첫 번째로
        document.querySelector('.sec-1-input').checked = true;
    }
}

// 자동 전환 타이머
let bannerTimer;

// 자동 전환 시작
function startBannerTimer() {
    bannerTimer = setInterval(bannerSwitcher, 5000); // 5초마다 전환
}

// 자동 전환 중지
function stopBannerTimer() {
    if (bannerTimer) {
        clearInterval(bannerTimer);
    }
}

// 자동 전환 재시작
function restartBannerTimer() {
    stopBannerTimer();
    startBannerTimer();
}

// 컨트롤 라벨 클릭 이벤트 (수동 전환 시 타이머 재시작)
function setupSliderControls() {
    const controlLabels = document.querySelectorAll('.controls label');
    
    controlLabels.forEach(label => {
        label.addEventListener('click', function() {
            restartBannerTimer();
        });
    });
}

// 슬라이더 버튼 클릭 이벤트 (2세트 기능과 연동)
function setupSliderButtons() {
    const sliderButtons = document.querySelectorAll('.learn-more-button a');
    
    sliderButtons.forEach(button => {
        // banking-btn이나 loan-btn 클래스가 있으면 2세트의 핸들러 사용
        if (button.classList.contains('banking-btn') || button.classList.contains('loan-btn')) {
            button.addEventListener('click', handleServiceButtonClick);
        } else {
            // 일반 링크는 기본 동작 (앵커 스크롤 등)
            button.addEventListener('click', function(e) {
                const href = this.getAttribute('href');
                if (href && href.startsWith('#')) {
                    e.preventDefault();
                    const target = document.querySelector(href);
                    if (target) {
                        target.scrollIntoView({ behavior: 'smooth' });
                    }
                }
            });
        }
    });
}

// 서비스 버튼 클릭 핸들러 (2세트 기능과 연동)
function handleServiceButtonClick(e) {
    e.preventDefault();
    
    const btn = e.currentTarget;
    const redirect = btn.dataset.redirect;
    
    // 토큰 확인 (auth.js의 함수 사용)
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
    
    // 토큰이 있으면 바로 이동
    window.location.href = redirect;
}

// 슬라이더 호버 시 자동 전환 일시정지
function setupSliderHover() {
    const slider = document.querySelector('#section-1');
    
    if (slider) {
        slider.addEventListener('mouseenter', stopBannerTimer);
        slider.addEventListener('mouseleave', startBannerTimer);
    }
}

// 슬라이더 초기화
function initializeSlider() {
    console.log('슬라이더 초기화 시작');
    
    // 첫 번째 배너 활성화
    const firstInput = document.querySelector('#banner1');
    if (firstInput) {
        firstInput.checked = true;
    }
    
    // 이벤트 설정
    setupSliderControls();
    setupSliderButtons();
    setupSliderHover();
    
    // 자동 전환 시작
    startBannerTimer();
    
    console.log('슬라이더 초기화 완료');
}

// 페이지 로드 완료 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    // 슬라이더가 있는 경우에만 초기화
    if (document.querySelector('#section-1')) {
        initializeSlider();
    }
});

// 페이지를 떠날 때 타이머 정리
window.addEventListener('beforeunload', function() {
    stopBannerTimer();
});

// 전역 함수로 노출 (다른 스크립트에서 사용 가능)
window.sliderFunctions = {
    start: startBannerTimer,
    stop: stopBannerTimer,
    restart: restartBannerTimer,
    switchBanner: bannerSwitcher
};