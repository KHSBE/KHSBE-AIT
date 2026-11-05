import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { exchangeLoginCode } from "../utils/api";
export default function OAuth2Redirect() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [failure, setError] = useState("");
  const error =
    failure ||
    (!params.get("code")
      ? "로그인 코드가 없습니다. 다시 로그인해 주세요."
      : "");
  useEffect(() => {
    let active = true;
    const code = params.get("code");
    if (!code) return;
    exchangeLoginCode(code)
      .then(() => {
        if (active) navigate("/planner", { replace: true });
      })
      .catch((e) => {
        if (active) setError(e.message);
      });
    return () => {
      active = false;
    };
  }, [params, navigate]);
  return (
    <div className="mx-auto max-w-md px-6 py-20 text-center">
      <h1 className="text-2xl font-bold">
        {error ? "로그인을 완료하지 못했어요" : "로그인 처리 중이에요"}
      </h1>
      <p className="mt-4 text-sm text-slate-500" role="status">
        {error || "잠시만 기다려 주세요."}
      </p>
      {error && (
        <Link to="/login" className="mt-6 inline-block text-blue-600">
          다시 로그인하기
        </Link>
      )}
    </div>
  );
}
