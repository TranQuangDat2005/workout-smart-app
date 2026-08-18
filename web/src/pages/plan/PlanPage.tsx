import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Button from '../../components/Button';
import { planApi } from '../../services/planApi';
import type { WorkoutPlan } from '../../services/planApi';

const DAY_LABELS = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];
const DAY_SHORT  = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
const GOAL_LABELS: Record<string, string> = {
  weight_loss: 'Giảm cân', muscle_gain: 'Tăng cơ', endurance: 'Sức bền',
};
const LEVEL_LABELS: Record<string, string> = {
  beginner: 'Mới bắt đầu', intermediate: 'Trung bình', advanced: 'Nâng cao',
};

export default function PlanPage() {
  const [plan, setPlan] = useState<WorkoutPlan | null>(null);
  const [error, setError] = useState('');
  const [activeDay, setActiveDay] = useState<number | null>(null);

  useEffect(() => {
    planApi
      .getActivePlan()
      .then((p) => {
        setPlan(p);
        setActiveDay(p.days[0]?.dayOfWeek ?? null);
      })
      .catch(() => setError('Chưa có lộ trình tập. Hãy thiết lập mục tiêu.'));
  }, []);

  if (error && !plan) {
    return (
      <div className="page-container" style={{ textAlign: 'center', paddingTop: 60 }}>
        <div style={{ fontSize: 56, marginBottom: 16 }}>📋</div>
        <h2 style={{ marginBottom: 8 }}>Chưa có lộ trình</h2>
        <p className="text-secondary text-sm" style={{ marginBottom: 24 }}>{error}</p>
        <Link to="/goal-setup">
          <Button size="lg">🎯 Thiết lập mục tiêu</Button>
        </Link>
      </div>
    );
  }

  const today = new Date().getDay();
  const selectedDay = plan?.days.find((d) => d.dayOfWeek === activeDay);

  return (
    <div className="page-container" style={{ maxWidth: 800, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Header */}
      <div className="page-header">
        <div>
          <h1>{plan?.name ?? 'Lộ trình tập'}</h1>
          <p className="text-secondary text-sm" style={{ marginTop: 4 }}>
            {plan?.goalType ? GOAL_LABELS[plan.goalType] ?? plan.goalType : ''}
            {plan?.fitnessLevel ? ` · ${LEVEL_LABELS[plan.fitnessLevel] ?? plan.fitnessLevel}` : ''}
            <span className="badge badge-green" style={{ marginLeft: 10 }}>Đang hoạt động</span>
          </p>
        </div>
        <Link to="/goal-setup">
          <Button variant="outlined" size="sm">Tạo lộ trình mới</Button>
        </Link>
      </div>

      {/* Day tabs */}
      {plan && (
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {plan.days.map((day) => (
            <button
              key={day.id}
              type="button"
              onClick={() => setActiveDay(day.dayOfWeek)}
              className={`btn btn-sm ${activeDay === day.dayOfWeek ? 'btn-primary' : 'btn-dark'}`}
              style={{ position: 'relative' }}
            >
              {DAY_SHORT[day.dayOfWeek]}
              {day.dayOfWeek === today && (
                <span
                  style={{
                    position: 'absolute', top: -4, right: -4,
                    width: 8, height: 8, borderRadius: '50%',
                    background: 'var(--green)', border: '2px solid var(--near-black)',
                  }}
                />
              )}
            </button>
          ))}
        </div>
      )}

      {/* Exercises for selected day */}
      {selectedDay ? (
        <div className="card animate-fade">
          <div className="section-header" style={{ marginBottom: 16 }}>
            <h2 className="section-title">
              {DAY_LABELS[selectedDay.dayOfWeek]}
              {selectedDay.dayOfWeek === today && <span className="badge badge-green" style={{ marginLeft: 10 }}>Hôm nay</span>}
            </h2>
            <span className="text-secondary text-sm">{selectedDay.exercises.length} bài tập</span>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Bài tập</th>
                <th>Sets × Reps</th>
                <th>Nghỉ (s)</th>
              </tr>
            </thead>
            <tbody>
              {selectedDay.exercises.map((ex, idx) => (
                <tr key={ex.id}>
                  <td><span className="text-muted fw-700">{idx + 1}</span></td>
                  <td className="fw-600">{ex.exerciseName}</td>
                  <td><span className="text-green fw-700">{ex.targetSets}</span> × {ex.targetReps}</td>
                  <td className="text-secondary">{ex.restTimeSeconds}s</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: 16 }}>
            <Link to="/workout">
              <Button fullWidth>💪 Bắt đầu tập ngay</Button>
            </Link>
          </div>
        </div>
      ) : (
        plan && (
          <div className="card">
            <div className="empty-state">
              <div className="empty-state-icon">🏖️</div>
              <p className="empty-state-text">Chọn một ngày để xem bài tập.</p>
            </div>
          </div>
        )
      )}
    </div>
  );
}
