export const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"
).replace(/\/$/, "");
export const buildApiUrl = (path) =>
  /^https?:\/\//.test(path)
    ? path
    : `${API_BASE_URL}${path.startsWith("/") ? "" : "/"}${path}`;
export const GOOGLE_OAUTH2_URL = buildApiUrl("/oauth2/authorization/google");
const decodeJwtPayload = (token) => {
  try {
    const base = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(
      new TextDecoder().decode(
        Uint8Array.from(atob(base), (c) => c.charCodeAt(0)),
      ),
    );
  } catch {
    return null;
  }
};
export const getAccessTokenRole = () =>
  String(
    decodeJwtPayload(localStorage.getItem("accessToken") || "")?.role || "USER",
  );
let refreshPromise;
const authPost = (path, body) =>
  fetch(`/api/auth/${path}`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: body ? JSON.stringify(body) : undefined,
  });
const refreshAccessToken = () => {
  if (!refreshPromise)
    refreshPromise = (async () => {
      const response = await authPost("refresh");
      if (response.status === 401) return null;
      if (!response.ok)
        throw new Error(
          "인증 서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.",
        );
      const result = await response.json();
      if (!result.data?.accessToken)
        throw new Error("인증 응답을 확인하지 못했습니다.");
      localStorage.setItem("accessToken", result.data.accessToken);
      return result.data.accessToken;
    })().finally(() => {
      refreshPromise = undefined;
    });
  return refreshPromise;
};
const codeExchanges = new Map();
export function exchangeLoginCode(code) {
  if (!codeExchanges.has(code))
    codeExchanges.set(
      code,
      (async () => {
        const response = await authPost("exchange", { code });
        const data = await response.json();
        if (!response.ok || !data.data?.accessToken)
          throw new Error(data.message || "로그인 처리에 실패했습니다.");
        localStorage.removeItem("refreshToken");
        localStorage.setItem("accessToken", data.data.accessToken);
      })(),
    );
  return codeExchanges.get(code);
}
export async function logout() {
  const response = await authPost("logout");
  if (!response.ok)
    throw new Error(
      "로그아웃 요청에 실패했습니다. 잠시 후 다시 시도해 주세요.",
    );
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
}
function redirectToLogin() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  window.location.assign("/login");
}
export async function fetchWithAuth(url, options = {}) {
  const requestUrl = buildApiUrl(url);
  let token = localStorage.getItem("accessToken");
  const payload = decodeJwtPayload(token || "");
  if (token && (!payload?.exp || payload.exp * 1000 < Date.now() + 30000)) {
    token = await refreshAccessToken();
    if (!token) {
      redirectToLogin();
      return new Response(null, { status: 401 });
    }
  }
  const send = (access) => {
    const headers = new Headers(options.headers);
    if (access) headers.set("Authorization", `Bearer ${access}`);
    return fetch(requestUrl, { ...options, headers });
  };
  let response = await send(token);
  if (response.status !== 401) return response;
  token = await refreshAccessToken();
  if (!token) {
    redirectToLogin();
    return response;
  }
  response = await send(token);
  if (response.status === 401) redirectToLogin();
  return response;
}
