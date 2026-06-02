// 오늘 날짜로 현재 학기를 자동 판단한다.
// 한국 대학 학사 기준: 3~6월=1학기, 7~8월=여름방학, 9~12월=2학기, 1~2월=겨울방학.
export const TERM_LABELS = {
  SPRING: "1학기",
  SUMMER: "여름방학",
  FALL: "2학기",
  WINTER: "겨울방학",
};

export function getCurrentSemester(date = new Date()) {
  const year = date.getFullYear();
  const m = date.getMonth() + 1; // 1~12
  let termSeason;
  if (m >= 3 && m <= 6) termSeason = "SPRING";
  else if (m >= 7 && m <= 8) termSeason = "SUMMER";
  else if (m >= 9 && m <= 12) termSeason = "FALL";
  else termSeason = "WINTER"; // 1~2월
  return { year, termSeason, label: `${year} ${TERM_LABELS[termSeason]}` };
}
