import { ArrowLeft, Navigation2 } from "lucide-react";
import { Link } from "react-router-dom";
import { GOOGLE_OAUTH2_URL } from "../utils/api";

export default function Login() {
  return (
    <div className="login-page app-container">
      <section className="login-panel reveal-up" aria-labelledby="login-title">
        <span className="login-symbol">
          <Navigation2 size={28} strokeWidth={1.6} />
        </span>
        <p className="eyebrow">여행 준비를 이어서</p>
        <h1 id="login-title">
          반가워요.
          <br />
          어디로 떠나 볼까요?
        </h1>
        <p className="login-description">
          Google 계정으로 로그인하면
          <br />
          일정을 만들고, 저장하고, 수정할 수 있어요.
        </p>
        <button
          type="button"
          className="google-login"
          onClick={() => {
            window.location.href = GOOGLE_OAUTH2_URL;
          }}
        >
          <svg width="19" height="19" viewBox="0 0 48 48" aria-hidden="true">
            <path
              fill="#EA4335"
              d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5Z"
            />
            <path
              fill="#4285F4"
              d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6C44.4 38.03 46.98 31.89 46.98 24.55Z"
            />
            <path
              fill="#FBBC05"
              d="M10.53 28.59A14.4 14.4 0 0 1 9.75 24c0-1.59.27-3.13.78-4.59l-7.98-6.19A23.86 23.86 0 0 0 0 24c0 3.87.93 7.53 2.56 10.78l7.97-6.19Z"
            />
            <path
              fill="#34A853"
              d="M24 48c6.48 0 11.92-2.13 15.89-5.8l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48Z"
            />
          </svg>
          Google로 계속하기
        </button>
        <Link to="/" className="login-back">
          <ArrowLeft size={15} /> 먼저 둘러보기
        </Link>
      </section>
      <aside className="login-aside">
        <span>YOUR NEXT CHAPTER</span>
        <p>
          계획하는 시간보다,
          <br />
          여행하는 시간을 더.
        </p>
        <span className="login-aside-line" />
        <p className="login-aside-caption">
          완벽한 정답 대신,
          <br />
          나에게 맞춰 바꿀 수 있는 여행 초안.
        </p>
      </aside>
    </div>
  );
}
