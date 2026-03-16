import { useState, useEffect } from "react";
import {
  listEvents,
  createEvent,
  deleteEvent,
  listTasks,
  createTask,
  deleteTask,
  updateTask,
} from "../api/calendar";
import { getAcademicCalendarEvents } from "../api/academic";

const ACADEMIC_COLORS = {
  SEMESTER: "#3ab87a",
  REGISTRATION: "#4a6fe3",
  EXAM: "#e35a4a",
  HOLIDAY: "#c94fb8",
};
const ACADEMIC_LABELS = {
  SEMESTER: "학기",
  REGISTRATION: "수강신제",
  EXAM: "고사",
  HOLIDAY: "휴일",
};

const CATEGORIES = [
  "CLASS",
  "ASSIGNMENT",
  "EXAM",
  "PERSONAL",
  "SCHOOL",
  "PROJECT",
];
const CATEGORY_LABELS = {
  CLASS: "수업",
  ASSIGNMENT: "과제",
  EXAM: "시험",
  PERSONAL: "개인",
  SCHOOL: "학교",
  PROJECT: "프로젝트",
};
const TASK_STATUSES = ["TODO", "IN_PROGRESS", "DONE"];
const STATUS_LABELS = { TODO: "예정", IN_PROGRESS: "진행 중", DONE: "완료" };

function MonthCalendar({
  year,
  month,
  events,
  tasks,
  academicEvents,
  selectedDate,
  onDayClick,
}) {
  const today = new Date();
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
  const firstDay = new Date(year, month, 1);
  const startDow = firstDay.getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const cells = [];
  for (let i = 0; i < startDow; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(d);
  while (cells.length % 7 !== 0) cells.push(null);

  const ds = (d) =>
    d
      ? `${year}-${String(month + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`
      : null;

  return (
    <div className="month-calendar">
      <div className="cal-dow-header">
        {["일", "월", "화", "수", "목", "금", "토"].map((d) => (
          <div key={d} className="cal-dow">
            {d}
          </div>
        ))}
      </div>
      <div className="cal-grid">
        {cells.map((d, i) => {
          const dateStr = ds(d);
          const isToday = dateStr === todayStr;
          const isSelected = dateStr === selectedDate;
          const dayAcademic = d
            ? (academicEvents || []).filter(
                (ae) => dateStr >= ae.startDate && dateStr <= ae.endDate,
              )
            : [];
          const dayEvs = d
            ? (events || []).filter(
                (ev) => ev.startAt && ev.startAt.startsWith(dateStr),
              )
            : [];
          const dayTks = d
            ? (tasks || []).filter(
                (t) =>
                  t.scheduledDate === dateStr ||
                  (t.dueAt && t.dueAt.startsWith(dateStr)),
              )
            : [];
          const shown = [];
          for (const ev of dayEvs) {
            if (shown.length >= 2) break;
            shown.push({ type: "event", item: ev });
          }
          if (shown.length < 2 && dayTks.length > 0) {
            shown.push({ type: "task", item: dayTks[0] });
          }
          const more = dayEvs.length + dayTks.length - shown.length;

          let cls = "cal-cell";
          if (!d) cls += " empty-cell";
          else {
            cls += " active-cell";
            if (isToday) cls += " today-cell";
            if (isSelected) cls += " selected-cell";
          }

          return (
            <div
              key={i}
              className={cls}
              onClick={() => d && onDayClick(dateStr)}
            >
              {d && (
                <>
                  {dayAcademic.length > 0 && (
                    <div
                      style={{
                        display: "flex",
                        flexWrap: "wrap",
                        gap: 2,
                        marginBottom: 3,
                      }}
                    >
                      {dayAcademic.slice(0, 2).map((ae) => (
                        <div
                          key={ae.id}
                          style={{
                            height: 4,
                            flex: "1 1 auto",
                            minWidth: 12,
                            borderRadius: 2,
                            background: ACADEMIC_COLORS[ae.category] || "#999",
                            title: ae.title,
                          }}
                          title={ae.title}
                        />
                      ))}
                    </div>
                  )}
                  <div
                    className="cal-day-num"
                    style={{
                      color:
                        i % 7 === 0
                          ? "#e35a4a"
                          : i % 7 === 6
                            ? "#4a6fe3"
                            : undefined,
                    }}
                  >
                    {d}
                  </div>
                  <div className="cal-dots">
                    {shown.map((s, si) =>
                      s.type === "event" ? (
                        <div
                          key={si}
                          className="cal-event-pill"
                          style={{ background: s.item.color || "#4f9cf9" }}
                        >
                          {s.item.title}
                        </div>
                      ) : (
                        <div key={si} className="cal-task-pill">
                          {s.item.title}
                        </div>
                      ),
                    )}
                    {more > 0 && (
                      <div className="cal-more-pill">+{more} 더보기</div>
                    )}
                  </div>
                </>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default function CalendarPage() {
  const [events, setEvents] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [academicEvents, setAcademicEvents] = useState([]);
  const _now = new Date();
  const [year, setYear] = useState(_now.getFullYear());
  const [month, setMonth] = useState(_now.getMonth());
  const [selectedDate, setSelectedDate] = useState(null);
  const [tab, setTab] = useState("calendar");
  const [showEventModal, setShowEventModal] = useState(false);
  const [showTaskModal, setShowTaskModal] = useState(false);
  const [eventForm, setEventForm] = useState({
    title: "",
    category: "PERSONAL",
    startAt: "",
    endAt: "",
    allDay: false,
    description: "",
    color: "#4f9cf9",
  });
  const [taskForm, setTaskForm] = useState({
    title: "",
    scheduledDate: "",
    dueAt: "",
    category: "ASSIGNMENT",
  });
  const [error, setError] = useState("");

  useEffect(() => {
    loadEvents();
    loadTasks();
    getAcademicCalendarEvents(2026)
      .then((r) => setAcademicEvents(r.data.data || []))
      .catch(() => setAcademicEvents([]));
  }, []);

  const loadEvents = async () => {
    try {
      const res = await listEvents();
      setEvents(res.data.data || []);
    } catch {
      setEvents([]);
    }
  };

  const loadTasks = async () => {
    try {
      const res = await listTasks();
      setTasks(res.data.data || []);
    } catch {
      setTasks([]);
    }
  };

  const handleCreateEvent = async (e) => {
    e.preventDefault();
    setError("");
    try {
      await createEvent(eventForm);
      setShowEventModal(false);
      setEventForm({
        title: "",
        category: "PERSONAL",
        startAt: "",
        endAt: "",
        allDay: false,
        description: "",
        color: "#4f9cf9",
      });
      loadEvents();
    } catch (err) {
      setError(err.response?.data?.message || "이벤트 생성 실패");
    }
  };

  const handleCreateTask = async (e) => {
    e.preventDefault();
    setError("");
    try {
      await createTask(taskForm);
      setShowTaskModal(false);
      setTaskForm({
        title: "",
        scheduledDate: "",
        dueAt: "",
        category: "ASSIGNMENT",
      });
      loadTasks();
    } catch (err) {
      setError(err.response?.data?.message || "태스크 생성 실패");
    }
  };

  const handleDeleteEvent = async (id) => {
    if (!window.confirm("삭제하시겠습니까?")) return;
    try {
      await deleteEvent(id);
      loadEvents();
    } catch {
      setError("삭제 실패");
    }
  };

  const handleDeleteTask = async (id) => {
    if (!window.confirm("삭제하시겠습니까?")) return;
    try {
      await deleteTask(id);
      loadTasks();
    } catch {
      setError("삭제 실패");
    }
  };

  const handleTaskStatusChange = async (id, status) => {
    try {
      await updateTask(id, { status });
      loadTasks();
    } catch {
      setError("상태 변경 실패");
    }
  };

  const handleDayClick = (d) =>
    setSelectedDate((prev) => (prev === d ? null : d));
  const prevMonth = () => {
    if (month === 0) {
      setYear((y) => y - 1);
      setMonth(11);
    } else setMonth((m) => m - 1);
  };
  const nextMonth = () => {
    if (month === 11) {
      setYear((y) => y + 1);
      setMonth(0);
    } else setMonth((m) => m + 1);
  };
  const MONTH_LABELS = [
    "1월",
    "2월",
    "3월",
    "4월",
    "5월",
    "6월",
    "7월",
    "8월",
    "9월",
    "10월",
    "11월",
    "12월",
  ];
  const dayEventsForPanel = selectedDate
    ? events.filter((ev) => ev.startAt && ev.startAt.startsWith(selectedDate))
    : [];
  const dayTasksForPanel = selectedDate
    ? tasks.filter(
        (t) =>
          t.scheduledDate === selectedDate ||
          (t.dueAt && t.dueAt.startsWith(selectedDate)),
      )
    : [];

  return (
    <div className="page-container">
      <div className="page-header">
        <h2>캘린더</h2>
        <div className="tab-group">
          <button
            className={`tab ${tab === "calendar" ? "active" : ""}`}
            onClick={() => setTab("calendar")}
          >
            달력
          </button>
          <button
            className={`tab ${tab === "events" ? "active" : ""}`}
            onClick={() => setTab("events")}
          >
            일정 목록
          </button>
          <button
            className={`tab ${tab === "tasks" ? "active" : ""}`}
            onClick={() => setTab("tasks")}
          >
            할 일 목록
          </button>
        </div>
        {(tab === "events" || tab === "calendar") && (
          <button
            className="btn-primary"
            onClick={() => setShowEventModal(true)}
          >
            + 일정 추가
          </button>
        )}
        {tab === "tasks" && (
          <button
            className="btn-primary"
            onClick={() => setShowTaskModal(true)}
          >
            + 할 일 추가
          </button>
        )}
      </div>

      {error && <p className="error">{error}</p>}

      {/* Monthly Calendar View */}
      {tab === "calendar" && (
        <>
          <div className="cal-nav">
            <button className="btn-secondary" onClick={prevMonth}>
              ‹
            </button>
            <h3>
              {year}년 {MONTH_LABELS[month]}
            </h3>
            <button className="btn-secondary" onClick={nextMonth}>
              ›
            </button>
            <button
              className="btn-secondary"
              onClick={() => {
                setYear(_now.getFullYear());
                setMonth(_now.getMonth());
              }}
            >
              오늘
            </button>
          </div>
          <div className="cal-legend">
            {Object.entries(ACADEMIC_LABELS).map(([cat, label]) => (
              <span key={cat} className="cal-legend-item">
                <span
                  className="cal-legend-dot"
                  style={{ background: ACADEMIC_COLORS[cat] }}
                />
                {label}
              </span>
            ))}
          </div>
          <div className="cal-layout">
            <MonthCalendar
              year={year}
              month={month}
              events={events}
              tasks={tasks}
              academicEvents={academicEvents}
              selectedDate={selectedDate}
              onDayClick={handleDayClick}
            />
            {selectedDate && (
              <div className="day-panel">
                <h4>{selectedDate}</h4>
                {dayEventsForPanel.length === 0 &&
                  dayTasksForPanel.length === 0 && (
                    <p
                      className="empty"
                      style={{ fontSize: "0.85rem", marginTop: 8 }}
                    >
                      일정이 없습니다.
                    </p>
                  )}
                {dayEventsForPanel.map((ev) => (
                  <div
                    key={ev.id}
                    className="day-item"
                    style={{
                      borderLeft: `3px solid ${ev.color || "#4f9cf9"}`,
                    }}
                  >
                    <span style={{ fontSize: "0.85rem" }}>{ev.title}</span>
                    <button
                      className="btn-danger-sm"
                      onClick={() => handleDeleteEvent(ev.id)}
                    >
                      삭제
                    </button>
                  </div>
                ))}
                {dayTasksForPanel.map((t) => (
                  <div
                    key={t.id}
                    className={`day-item ${t.status === "DONE" ? "done" : ""}`}
                  >
                    <span style={{ fontSize: "0.85rem" }}>{t.title}</span>
                    <button
                      className="btn-danger-sm"
                      onClick={() => handleDeleteTask(t.id)}
                    >
                      삭제
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}

      {/* Event Modal */}
      {showEventModal && (
        <div className="modal-overlay" onClick={() => setShowEventModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>일정 추가</h3>
              <button
                className="btn-icon-close"
                onClick={() => setShowEventModal(false)}
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCreateEvent} className="form-grid">
              <div className="field">
                <label>제목</label>
                <input
                  value={eventForm.title}
                  onChange={(e) =>
                    setEventForm({ ...eventForm, title: e.target.value })
                  }
                  required
                />
              </div>
              <div className="field">
                <label>카테고리</label>
                <select
                  value={eventForm.category}
                  onChange={(e) =>
                    setEventForm({ ...eventForm, category: e.target.value })
                  }
                >
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {CATEGORY_LABELS[c]}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>시작</label>
                <input
                  type="datetime-local"
                  value={eventForm.startAt}
                  onChange={(e) =>
                    setEventForm({ ...eventForm, startAt: e.target.value })
                  }
                  required
                />
              </div>
              <div className="field">
                <label>종료</label>
                <input
                  type="datetime-local"
                  value={eventForm.endAt}
                  onChange={(e) =>
                    setEventForm({ ...eventForm, endAt: e.target.value })
                  }
                  required
                />
              </div>
              <div className="field">
                <label>설명</label>
                <input
                  value={eventForm.description}
                  onChange={(e) =>
                    setEventForm({ ...eventForm, description: e.target.value })
                  }
                />
              </div>
              <div className="field">
                <label>색상</label>
                <input
                  type="color"
                  value={eventForm.color}
                  onChange={(e) =>
                    setEventForm({ ...eventForm, color: e.target.value })
                  }
                />
              </div>
              <div className="form-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setShowEventModal(false)}
                >
                  취소
                </button>
                <button type="submit" className="btn-primary">
                  저장
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Task Modal */}
      {showTaskModal && (
        <div className="modal-overlay" onClick={() => setShowTaskModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>할 일 추가</h3>
              <button
                className="btn-icon-close"
                onClick={() => setShowTaskModal(false)}
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCreateTask} className="form-grid">
              <div className="field">
                <label>제목</label>
                <input
                  value={taskForm.title}
                  onChange={(e) =>
                    setTaskForm({ ...taskForm, title: e.target.value })
                  }
                  required
                />
              </div>
              <div className="field">
                <label>카테고리</label>
                <select
                  value={taskForm.category}
                  onChange={(e) =>
                    setTaskForm({ ...taskForm, category: e.target.value })
                  }
                >
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {CATEGORY_LABELS[c]}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label>예정일</label>
                <input
                  type="date"
                  value={taskForm.scheduledDate}
                  onChange={(e) =>
                    setTaskForm({ ...taskForm, scheduledDate: e.target.value })
                  }
                />
              </div>
              <div className="field">
                <label>마감일시</label>
                <input
                  type="datetime-local"
                  value={taskForm.dueAt}
                  onChange={(e) =>
                    setTaskForm({ ...taskForm, dueAt: e.target.value })
                  }
                />
              </div>
              <div className="form-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setShowTaskModal(false)}
                >
                  취소
                </button>
                <button type="submit" className="btn-primary">
                  저장
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Events List */}
      {tab === "events" && (
        <div className="list-section">
          {events.length === 0 ? (
            <p className="empty">등록된 일정이 없습니다.</p>
          ) : (
            events.map((ev) => (
              <div
                key={ev.id}
                className="list-item"
                style={{ borderLeft: `4px solid ${ev.color || "#4f9cf9"}` }}
              >
                <div className="item-main">
                  <span className="badge">
                    {CATEGORY_LABELS[ev.category] || ev.category}
                  </span>
                  <strong>{ev.title}</strong>
                  <span className="item-meta">
                    {ev.startAt?.replace("T", " ").slice(0, 16)} ~{" "}
                    {ev.endAt?.replace("T", " ").slice(0, 16)}
                  </span>
                </div>
                <button
                  className="btn-danger-sm"
                  onClick={() => handleDeleteEvent(ev.id)}
                >
                  삭제
                </button>
              </div>
            ))
          )}
        </div>
      )}

      {/* Tasks List */}
      {tab === "tasks" && (
        <div className="list-section">
          {tasks.length === 0 ? (
            <p className="empty">등록된 할 일이 없습니다.</p>
          ) : (
            tasks.map((task) => (
              <div
                key={task.id}
                className={`list-item ${task.status === "DONE" ? "done" : ""}`}
              >
                <div className="item-main">
                  <span className="badge">
                    {CATEGORY_LABELS[task.category] || task.category}
                  </span>
                  <strong>{task.title}</strong>
                  {task.dueAt && (
                    <span className="item-meta">
                      마감: {task.dueAt.replace("T", " ").slice(0, 16)}
                    </span>
                  )}
                </div>
                <div className="item-actions">
                  <select
                    value={task.status}
                    onChange={(e) =>
                      handleTaskStatusChange(task.id, e.target.value)
                    }
                    className="status-select"
                  >
                    {TASK_STATUSES.map((s) => (
                      <option key={s} value={s}>
                        {STATUS_LABELS[s]}
                      </option>
                    ))}
                  </select>
                  <button
                    className="btn-danger-sm"
                    onClick={() => handleDeleteTask(task.id)}
                  >
                    삭제
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
