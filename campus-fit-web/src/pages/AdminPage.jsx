import { useState, useEffect } from "react";
import {
  listVerifications,
  reviewVerification,
  getVerificationFileUrl,
} from "../api/admin";

const STATUS_LABELS = { PENDING: "대기", APPROVED: "승인", REJECTED: "반려" };
const STATUS_COLORS = {
  PENDING: "#f9c74f",
  APPROVED: "#4fc97a",
  REJECTED: "#f94f4f",
};

export default function AdminPage() {
  const [verifications, setVerifications] = useState([]);
  const [filter, setFilter] = useState("");
  const [selected, setSelected] = useState(null);
  const [rejectReason, setRejectReason] = useState("");
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    loadList();
  }, [filter]);

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
        <h2>재학생 인증 관리</h2>
        <div className="tab-group">
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
      </div>

      {msg && <p className="success">{msg}</p>}
      {error && <p className="error">{error}</p>}

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
    </div>
  );
}
