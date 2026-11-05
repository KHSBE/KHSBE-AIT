import { Link } from "react-router-dom";
import {
  ArrowUpRight,
  ArrowRight,
  MapPin,
  Clock3,
  SlidersHorizontal,
  CalendarDays,
} from "lucide-react";

const exampleStops = [
  {
    time: "10:00",
    name: "오사카 역사박물관",
    type: "관광",
    stay: "90분",
    note: "도시의 이야기를 먼저 만나고",
  },
  {
    time: "12:00",
    name: "오사카성 공원",
    type: "산책",
    stay: "120분",
    note: "천천히 걸으며 쉬어 가기",
  },
  {
    time: "15:00",
    name: "나카자키초",
    type: "동네 탐방",
    stay: "90분",
    note: "골목과 카페에서 오후 보내기",
  },
];

export default function Home() {
  const isLoggedIn = !!localStorage.getItem("accessToken");
  return (
    <div className="home-page app-container">
      <section className="home-intro" aria-labelledby="home-title">
        <div className="home-copy reveal-up">
          <p className="eyebrow">
            <span className="status-dot" /> 다음 여행의 시작
          </p>
          <h1 id="home-title">
            가고 싶은 곳에서,
            <br />갈 수 있는 일정으로.
          </h1>
          <p className="home-description">
            어디로, 누구와, 어떻게 떠날지만 골라 주세요.
            <br className="desktop-break" /> 장소 정보를 확인한 여행 초안을
            만들고,
            <br className="desktop-break" /> 내 취향대로 바꿔 갈 수 있어요.
          </p>
          <Link
            to={isLoggedIn ? "/planner" : "/login"}
            className="button button-primary home-start"
          >
            여행 만들기 <ArrowUpRight size={19} />
          </Link>
          <p className="home-start-note">
            여행지부터 취향까지, 다섯 단계로 준비해요.
          </p>
          <Link
            to={isLoggedIn ? "/mypage" : "/login"}
            className="home-return-link"
          >
            {isLoggedIn
              ? "저장해 둔 내 일정 열기"
              : "이미 만든 일정이 있나요? 로그인"}{" "}
            <ArrowRight size={15} />
          </Link>
        </div>
        <div
          className="home-example reveal-up"
          aria-label="일정 화면 예시. 실제 생성 또는 검증된 일정이 아닙니다."
        >
          <div className="example-label">
            <span>이렇게 정리돼요</span>
            <span className="sample-tag">화면 예시</span>
          </div>
          <div className="example-heading">
            <div>
              <p className="example-city">
                <MapPin size={14} /> 일본 · 오사카
              </p>
              <h2>나만의 속도로, 오사카</h2>
            </div>
            <span className="example-duration">2박 3일</span>
          </div>
          <div className="example-days" aria-label="예시의 첫째 날">
            <span className="is-selected">Day 1</span>
            <span>Day 2</span>
            <span>Day 3</span>
          </div>
          <ol className="example-stops">
            {exampleStops.map((stop, index) => (
              <li key={stop.name}>
                <div className="example-time">{stop.time}</div>
                <span className="example-order">{index + 1}</span>
                <div className="example-stop-content">
                  <h3>{stop.name}</h3>
                  <p>{stop.note}</p>
                  <div className="example-meta">
                    <span>{stop.type}</span>
                    <span>
                      <Clock3 size={12} /> 예정 체류 {stop.stay}
                    </span>
                  </div>
                </div>
              </li>
            ))}
          </ol>
          <p className="example-disclaimer">
            구성을 보여 주기 위한 예시예요. 영업시간·동선은 검증하지 않았어요.
          </p>
        </div>
      </section>
      <section className="home-how" aria-labelledby="home-how-title">
        <div className="home-how-heading">
          <p className="eyebrow">계획은 간결하게</p>
          <h2 id="home-how-title">
            찾아보고, 고치고,
            <br />내 여행으로.
          </h2>
        </div>
        <div className="home-how-items">
          <div>
            <CalendarDays size={20} strokeWidth={1.6} />
            <h3>나에게 맞는 초안</h3>
            <p>
              날짜와 동행, 여행 속도를 반영해
              <br />
              하루씩 보기 쉽게 정리해요.
            </p>
          </div>
          <div>
            <MapPin size={20} strokeWidth={1.6} />
            <h3>장소를 함께 확인</h3>
            <p>
              장소 정보와 지도 출처를 살펴보고,
              <br />
              실제 길찾기는 Google Maps에서.
            </p>
          </div>
          <div>
            <SlidersHorizontal size={20} strokeWidth={1.6} />
            <h3>마지막 선택은 직접</h3>
            <p>
              마음에 드는 장소는 남기고,
              <br />
              순서와 장소를 바꿔 완성해요.
            </p>
          </div>
        </div>
      </section>
      <footer className="home-footer">
        <span>Compass · 나에게 맞는 여행의 방향</span>
        <span>영업시간과 예약 여부는 방문 전 다시 확인해 주세요.</span>
      </footer>
    </div>
  );
}
