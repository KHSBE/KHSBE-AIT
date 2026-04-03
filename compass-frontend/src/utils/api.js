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
  let accessToken = localStorage.getItem('accessToken');

  const headers = {
    ...options.headers
  };

  if (accessToken) {
    headers.Authorization = `Bearer ${accessToken}`;
  }

  let response = await fetch(requestUrl, { ...options, headers });

  if (response.status !== 401) {
    return response;
  }

  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) {
    localStorage.clear();
    window.location.href = '/login';
    return response;
  }

  try {
    const refreshResponse = await fetch(buildApiUrl('/api/auth/refresh'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });

    const refreshData = await refreshResponse.json();

    if (!refreshData.success || !refreshData.data?.accessToken) {
      throw new Error('토큰 갱신 실패');
    }

    accessToken = refreshData.data.accessToken;
    localStorage.setItem('accessToken', accessToken);

    const retryHeaders = {
      ...options.headers,
      Authorization: `Bearer ${accessToken}`
    };

    response = await fetch(requestUrl, { ...options, headers: retryHeaders });
    return response;
  } catch {
    localStorage.clear();
    alert('로그인이 만료되었습니다. 다시 로그인해 주세요.');
    window.location.href = '/login';
    return response;
  }
};
