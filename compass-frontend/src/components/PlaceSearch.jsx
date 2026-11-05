import { useState } from "react";
import { Search, Loader2, MapPin, X } from "lucide-react";
import { searchPlacesNew } from "../utils/places";

export default function PlaceSearch({
  label,
  value,
  onChange,
  ready,
  city = false,
  lodging = false,
  hint,
}) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const search = async () => {
    if (!ready || query.trim().length < 2 || busy) return;
    setBusy(true);
    setError("");
    setResults([]);
    try {
      let found = await searchPlacesNew(
        query.trim(),
        lodging ? { includedType: "lodging" } : {},
      );
      if (city)
        found = found.filter((p) =>
          p.types.some((type) =>
            [
              "locality",
              "administrative_area_level_1",
              "administrative_area_level_2",
              "postal_town",
            ].includes(type),
          ),
        );
      setResults(found.slice(0, 6));
      if (!found.length)
        setError(
          city
            ? "도시 이름과 국가를 함께 입력해 보세요. 예: 일본 오사카"
            : "검색 결과가 없습니다. 구체적인 장소명을 입력해 주세요.",
        );
    } catch {
      setError("장소 검색에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setBusy(false);
    }
  };
  return (
    <div className="place-search">
      <label className="field-title" htmlFor={`search-${label}`}>
        {label}
      </label>
      {value ? (
        <div className="selected-place">
          <MapPin size={18} />
          <span>
            <strong>{value.name}</strong>
            <small>{value.formatted_address}</small>
          </span>
          <button
            type="button"
            onClick={() => {
              onChange(null);
              setResults([]);
            }}
            aria-label={`${label} 선택 취소`}
          >
            <X size={18} />
          </button>
        </div>
      ) : (
        <>
          <div className="place-search-input">
            <input
              id={`search-${label}`}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  search();
                }
              }}
              placeholder={
                city
                  ? "도시 검색 · 예: 부산, 일본 오사카"
                  : "장소 이름과 지역을 함께 검색"
              }
              autoComplete="off"
            />
            <button
              type="button"
              disabled={!ready || busy || query.trim().length < 2}
              onClick={search}
            >
              {busy ? (
                <Loader2 size={18} className="animate-spin" />
              ) : (
                <Search size={18} />
              )}{" "}
              검색
            </button>
          </div>
          {hint && <p className="field-hint">{hint}</p>}
          {(!!results.length || !!value) && (
            <ul className="place-results">
              {results.map((p) => (
                <li key={p.place_id}>
                  <button
                    type="button"
                    onClick={() => {
                      onChange(p);
                      setResults([]);
                    }}
                  >
                    <MapPin size={17} />
                    <span>
                      <strong>{p.name}</strong>
                      <small>{p.formatted_address}</small>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
      {error && (
        <p className="field-error" role="alert">
          {error}
        </p>
      )}
      {(!!results.length || !!value) && (
        <p className="maps-attribution" translate="no">
          Google Maps
        </p>
      )}
    </div>
  );
}
