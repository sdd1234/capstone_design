import { useState, useEffect } from "react";
import {
  listVerifications,
  reviewVerification,
  getVerificationFileUrl,
} from "../api/admin";
import { importLectures, getImportHistory } from "../api/academic";

const STATUS_LABELS = { PENDING: "대기", APPROVED: "승인", REJECTED: "반려" };
const STATUS_COLORS = {
  PENDING: "#f9c74f",
  APPROVED: "#4fc97a",
  REJECTED: "#f94f4f",
};

export default function AdminPage() {
  const [mainTab, setMainTab] = useState("verifications");
  const [verifications, setVerifications] = useState([]);
  const [filter, setFilter] = useState("");
  const [selected, setSelected] = useState(null);
  const [rejectReason, setRejectReason] = useState("");
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");

  // 엑셀 업로드
  const [excelFile, setExcelFile] = useState(null);
  const [excelYear, setExcelYear] = useState(2026);
  const [excelTerm, setExcelTerm] = useState("SPRING");
  const [excelUnivId, setExcelUnivId] = useState(1);
  const [importMsg, setImportMsg] = useState("");
  const [importError, setImportError] = useState("");
  const [importing, setImporting] = useState(false);
  const [importHistory, setImportHistory] = useState([]);

  useEffect(() => {
    loadList();
  }, [filter]);

  useEffect(() => {
    if (mainTab === "excel") loadImportHistory();
  }, [mainTab]);

  const loadImportHistory = async () => {
    try {
      const res = await getImportHistory();
      setImportHistory(res.data.data || []);
    } catch {
      setImportHistory([]);
    }
  };

  const handleImport = async (e) => {
    e.preventDefault();
    if (!excelFile) {
      setImportError("파일을 선택하세요.");
      return;
    }
    setImporting(true);
    setImportMsg("");
    setImportError("");
    try {
      const fd = new FormData();
      fd.append("file", excelFile);
      fd.append("universityId", excelUnivId);
      fd.append("year", excelYear);
      fd.append("termSeason", excelTerm);
      const res = await importLectures(fd);
      setImportMsg(`✅ ${res.data.data.imported}건 등록 완료!`);
      setExcelFile(null);
      loadImportHistory();
    } catch (err) {
      setImportError(err.response?.data?.message || "업로드 실패");
    } finally {
      setImporting(false);
    }
  };

  const loadList = async () => {
    try {
      const res = await listVerifications(filter || undefined);
      setVerifications(res.data.data || []);
    } catch {
      setVerifications([]);
    }
  };

  const handleReview = async (id, status) => {
    setError("");
    setMsg("");
    if (status === "REJECTED" && !rejectReason.trim()) {
      setError("반려 사유를 입력하세요.");
      return;
    }
    try {
      await reviewVerification(id, {
        status,
        rejectReason: status === "REJECTED" ? rejectReason : null,
      });
      setMsg(`처리 완료: ${STATUS_LABELS[status]}`);
      setRejectReason("");
      setSelected(null);
      loadList();
    } catch (err) {
      setError(err.response?.data?.message || "처리 실패");
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>관리자</h2>
        <div className="tab-group">
          <button
            className={`tab ${mainTab === "verifications" ? "active" : ""}`}
            onClick={() => setMainTab("verifications")}
          >
            재학생 인증
          </button>
          <button
            className={`tab ${mainTab === "excel" ? "active" : ""}`}
            onClick={() => setMainTab("excel")}
          >
            강의 업로드
          </button>
        </div>
      </div>

      {mainTab === "excel" && (
        <div>
          <form
            onSubmit={handleImport}
            className="form-grid"
            style={{ maxWidth: 480 }}
          >
            <div className="field">
              <label>엑셀 파일 (.xlsx)</label>
              <input
                type="file"
                accept=".xlsx"
                onChange={(e) => setExcelFile(e.target.files[0])}
              />
            </div>
            <div className="field">
              <label>연도</label>
              <input
                type="number"
                value={excelYear}
                onChange={(e) => setExcelYear(Number(e.target.value))}
              />
            </div>
            <div className="field">
              <label>학기</label>
              <select
                value={excelTerm}
                onChange={(e) => setExcelTerm(e.target.value)}
              >
                <option value="SPRING">1학기 (SPRING)</option>
                <option value="FALL">2학기 (FALL)</option>
                <option value="SUMMER">여름학기 (SUMMER)</option>
                <option value="WINTER">겨울학기 (WINTER)</option>
              </select>
            </div>
            <div className="field">
              <label>대학교 ID</label>
              <input
                type="number"
                value={excelUnivId}
                onChange={(e) => setExcelUnivId(Number(e.target.value))}
              />
            </div>
            {importMsg && <p className="success">{importMsg}</p>}
            {importError && <p className="error">{importError}</p>}
            <button type="submit" className="btn-primary" disabled={importing}>
              {importing ? "업로드 중..." : "업로드"}
            </button>
          </form>

          <h3 style={{ marginTop: 32 }}>업로드 이력</h3>
          {importHistory.length === 0 ? (
            <p className="empty">이력 없음</p>
          ) : (
            <table
              className="detail-table"
              style={{ width: "100%", maxWidth: 720 }}
            >
              <thead>
                <tr>
                  <th>파일명</th>
                  <th>연도</th>
                  <th>학기</th>
                  <th>등록수</th>
                  <th>일시</th>
                </tr>
              </thead>
              <tbody>
                {importHistory.map((h) => (
                  <tr key={h.id}>
                    <td>{h.fileName}</td>
                    <td>{h.year}</td>
                    <td>{h.termSeason}</td>
                    <td>{h.importedCount}건</td>
                    <td>{h.importedAt?.slice(0, 16).replace("T", " ")}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {mainTab === "verifications" && (
        <>
          {msg && <p className="success">{msg}</p>}
          {error && <p className="error">{error}</p>}
          <div className="tab-group" style={{ marginBottom: 12 }}>
            {["", "PENDING", "APPROVED", "REJECTED"].map((s) => (
              <button
                key={s}
                className={`tab ${filter === s ? "active" : ""}`}
                onClick={() => setFilter(s)}
              >
                {s ? STATUS_LABELS[s] : "전체"}
              </button>
            ))}
          </div>
          <div className="split-layout">
            <div className="list-panel">
              {verifications.length === 0 && (
                <p className="empty">내역이 없습니다.</p>
              )}
              {verifications.map((v) => (
                <div
                  key={v.id}
                  className={`list-item clickable ${selected?.id === v.id ? "active" : ""}`}
                  onClick={() => {
                    setSelected(v);
                    setRejectReason("");
                    setMsg("");
                    setError("");
                  }}
                >
                  <div className="item-main">
                    <strong>{v.userName}</strong>
                    <span className="item-meta">{v.userEmail}</span>
                    <span
                      className="badge"
                      style={{ backgroundColor: STATUS_COLORS[v.status] }}
                    >
                      {STATUS_LABELS[v.status] || v.status}
                    </span>
                  </div>
                  <span className="item-date">{v.createdAt?.slice(0, 10)}</span>
                </div>
              ))}
            </div>

            {selected && (
              <div className="detail-panel">
                <h3>인증 상세</h3>
                <table className="detail-table">
                  <tbody>
                    <tr>
                      <th>이름</th>
                      <td>{selected.userName}</td>
                    </tr>
                    <tr>
                      <th>이메일</th>
                      <td>{selected.userEmail}</td>
                    </tr>
                    <tr>
                      <th>상태</th>
                      <td>
                        <span
                          className="badge"
                          style={{
                            backgroundColor: STATUS_COLORS[selected.status],
                          }}
                        >
                          {STATUS_LABELS[selected.status] || selected.status}
                        </span>
                      </td>
                    </tr>
                    <tr>
                      <th>신청일</th>
                      <td>{selected.createdAt?.slice(0, 10)}</td>
                    </tr>
                    {selected.rejectReason && (
                      <tr>
                        <th>반려 사유</th>
                        <td>{selected.rejectReason}</td>
                      </tr>
                    )}
                  </tbody>
                </table>

                {selected.fileId && (
                  <div className="file-preview">
                    <a
                      href={`${getVerificationFileUrl(selected.id)}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="btn-secondary"
                    >
                      인증 파일 보기
                    </a>
                  </div>
                )}

                {selected.status === "PENDING" && (
                  <div className="review-actions">
                    <div className="field">
                      <label>반려 사유 (반려 시 필수)</label>
                      <input
                        value={rejectReason}
                        onChange={(e) => setRejectReason(e.target.value)}
                        placeholder="반려 사유 입력"
                      />
                    </div>
                    <div className="btn-group">
                      <button
                        className="btn-success"
                        onClick={() => handleReview(selected.id, "APPROVED")}
                      >
                        승인
                      </button>
                      <button
                        className="btn-danger"
                        onClick={() => handleReview(selected.id, "REJECTED")}
                      >
                        반려
                      </button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
