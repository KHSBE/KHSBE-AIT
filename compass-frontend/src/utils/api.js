// src/utils/api.js
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const buildApiUrl = (path) => {
    if (path.startsWith('http://') || path.startsWith('https://')) {
        return path;
    }
    return `${API_BASE_URL}${path.startsWith('/') ? '' : '/'}${path}`;
};

export const GOOGLE_OAUTH2_URL = buildApiUrl('/oauth2/authorization/google');

export const fetchWithAuth = async (url, options = {}) => {
    const requestUrl = buildApiUrl(url);
    let token = localStorage.getItem('accessToken');

    // 헤더에 토큰 세팅
    let headers = {
        ...options.headers,
        'Authorization': `Bearer ${token}`
    };

    // 1차 API 호출
    let response = await fetch(requestUrl, { ...options, headers });

    // ★ 401(토큰 만료) 에러가 발생했다면?! (몰래 토큰 갱신 시도)
    if (response.status === 401) {
        const refreshToken = localStorage.getItem('refreshToken');

        // Refresh Token마저 없다면 얄짤없이 쫓아냄
        if (!refreshToken) {
            localStorage.clear();
            window.location.href = '/login';
            return response;
        }

        try {
            // 백엔드에 토큰 갱신 요청
            const refreshRes = await fetch(buildApiUrl('/api/auth/refresh'), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken })
            });

            const refreshData = await refreshRes.json();

            if (refreshData.success) {
                // 갱신 성공! 새 토큰을 저장하고 원래 하려던 API를 다시 쏜다!
                token = refreshData.data.accessToken;
                localStorage.setItem('accessToken', token);

                headers['Authorization'] = `Bearer ${token}`;
                response = await fetch(requestUrl, { ...options, headers }); // 2차 재시도
            } else {
                // 갱신 실패 (Refresh 토큰도 14일 지나서 죽음)
                throw new Error('Refresh failed');
            }
        } catch (error) {
            localStorage.clear();
            alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
            window.location.href = '/login';
        }
    }

    return response;
};
