export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const buildApiUrl = (path) => {
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path;
  }
  return `${API_BASE_URL}${path.startsWith('/') ? '' : '/'}${path}`;
};

export const GOOGLE_OAUTH2_URL = buildApiUrl('/oauth2/authorization/google');

const decodeJwtPayload = (token) => {
  if (!token) return null;

  try {
    const payload = token.split('.')[1];
    if (!payload) return null;

    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(normalizedPayload)
        .split('')
        .map((char) => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`)
        .join('')
    );

    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
};

const isAccessTokenExpired = (token) => {
  const payload = decodeJwtPayload(token);
  if (!payload?.exp) return false;

  const expiresAt = payload.exp * 1000;
  const refreshBufferMs = 30 * 1000;
  return Date.now() + refreshBufferMs >= expiresAt;
};

export const getAccessTokenRole = () => {
  const payload = decodeJwtPayload(localStorage.getItem('accessToken'));
  return String(payload?.role || 'USER').toUpperCase();
};

const refreshAccessToken = async () => {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) return null;

  const refreshResponse = await fetch(buildApiUrl('/api/auth/refresh'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });

  const refreshData = await refreshResponse.json();

  if (!refreshResponse.ok || !refreshData.success || !refreshData.data?.accessToken) {
    return null;
  }

  localStorage.setItem('accessToken', refreshData.data.accessToken);
  return refreshData.data.accessToken;
};

const redirectToLogin = () => {
  localStorage.clear();
  window.location.href = '/login';
};

export const fetchWithAuth = async (url, options = {}) => {
  const requestUrl = buildApiUrl(url);
  let accessToken = localStorage.getItem('accessToken');

  if (accessToken && isAccessTokenExpired(accessToken)) {
    accessToken = await refreshAccessToken();
    if (!accessToken) {
      redirectToLogin();
      return new Response(null, { status: 401 });
    }
  }

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

  try {
    accessToken = await refreshAccessToken();
    if (!accessToken) {
      redirectToLogin();
      return response;
    }

    const retryHeaders = {
      ...options.headers,
      Authorization: `Bearer ${accessToken}`
    };

    response = await fetch(requestUrl, { ...options, headers: retryHeaders });
    return response;
  } catch {
    alert('로그인이 만료되었습니다. 다시 로그인해 주세요.');
    redirectToLogin();
    return response;
  }
};
