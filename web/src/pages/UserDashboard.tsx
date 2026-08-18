import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Icon from '../components/Icon';
import { planApi } from '../services/planApi';
import type { WorkoutPlan } from '../services/planApi';
import { profileApi } from '../services/profileApi';
import type { Profile, WorkoutSessionItem } from '../services/profileApi';
import { statsApi } from '../services/statsApi';
import type { StatsDashboard } from '../services/statsApi';
import { nutritionApi } from '../services/nutritionApi';
import type { NutritionSummary } from '../services/nutritionApi';
import { todayLocalISO } from '../services/date';

const DAY_LABELS = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];

const GOAL_LABELS: Record<string, string> = {
  weight_loss: 'Giảm cân',
  muscle_gain: 'Tăng cơ',
  endurance: 'Sức bền',
};

function getGreeting() {
  const h = new Date().getHours();
  if (h < 12) return 'Chào buổi sáng';
  if (h < 18) return 'Chào buổi chiều';
  return 'Chào buổi tối';
}

/** Dashboard người dùng — tổng quan mục tiêu, streak, hôm nay tập gì, dinh dưỡng. */
export default function UserDashboard() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [plan, setPlan] = useState<WorkoutPlan | null>(null);
  const [stats, setStats] = useState<StatsDashboard | null>(null);
  const [sessions, setSessions] = useState<WorkoutSessionItem[]>([]);
  const [summary, setSummary] = useState<NutritionSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    void Promise.all([
      profileApi.getProfile().then(setProfile).catch(() => {}),
      planApi.getActivePlan().then(setPlan).catch(() => setPlan(null)),
      statsApi.dashboard().then(setStats).catch(() => {}),
      profileApi.getSessions(0, 5).then((r) => setSessions(r.content)).catch(() => {}),
      nutritionApi.getSummary(todayLocalISO()).then(setSummary).catch(() => {}),
    ]).finally(() => setLoading(false));
  }, []);

  const today = new Date().getDay();
  const todayDay = plan?.days.find((d) => d.dayOfWeek === today);
  const caloTarget = summary?.targetCalories ?? 0;
  const caloConsumed = summary?.totalCalories ?? 0;
  const caloPct = caloTarget > 0 ? Math.min(100, Math.round((caloConsumed / caloTarget) * 100)) : 0;
  const completionPct = stats?.planCompletion.completionPct ?? 0;

  if (loading) {
    return (
      <div className="page-container" style={{ maxWidth: 960, display: 'flex', justifyContent: 'center', paddingTop: 80 }}>
        <div className="btn-spinner" style={{ width: 28, height: 28, borderWidth: 3, color: 'var(--green)' }} aria-label="Đang tải" />
      </div>
    );
  }

  return (
    <div className="page-container" style={{ maxWidth: 960, display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Greeting */}
      <div>
        <h1 style={{ fontSize: 28 }}>
          {getGreeting()},{' '}
          <span style={{ color: 'var(--green)' }}>{profile?.displayName ?? 'bạn'}</span>! 👋
        </h1>
        <p className="text-secondary text-sm" style={{ marginTop: 4 }}>
          {profile?.goalType ? `Mục tiêu: ${GOAL_LABELS[profile.goalType] ?? profile.goalType}` : 'Hãy thiết lập mục tiêu để bắt đầu'}
          {profile?.weightKg != null && ` · ${profile.weightKg} kg`}
          {profile?.fitnessLevel && ` · Trình độ: ${profile.fitnessLevel}`}
        </p>
      </div>

      {/* Stat cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 12 }}>
        <div className="stat-card">
          <div className="stat-icon stat-icon-green"><Icon name="flame" /></div>
          <div className="stat-value text-green">{stats?.streak.currentStreakWeeks ?? 0}</div>
          <div className="stat-label">Tuần streak</div>
          {(stats?.streak.longestStreakWeeks ?? 0) > 0 && (
            <div className="stat-change text-muted" style={{ fontSize: 11 }}>
              Kỷ lục: {stats!.streak.longestStreakWeeks} tuần
            </div>
          )}
        </div>
        <div className="stat-card">
          <div className="stat-icon stat-icon-blue"><Icon name="chart" /></div>
          <div className="stat-value">{completionPct}%</div>
          <div className="stat-label">Hoàn thành lộ trình</div>
          <div className="progress-bar" style={{ marginTop: 8 }}>
            <div className="progress-fill" style={{ width: `${completionPct}%` }} />
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon stat-icon-orange"><Icon name="apple" /></div>
          <div className="stat-value">{caloConsumed}</div>
          <div className="stat-label">Calo hôm nay</div>
          {caloTarget > 0 && (
            <>
              <div className="progress-bar" style={{ marginTop: 8 }}>
                <div
                  className="progress-fill"
                  style={{
                    width: `${caloPct}%`,
                    background: caloPct > 100 ? 'var(--text-warning)' : 'var(--green)',
                  }}
                />
              </div>
              <div className="stat-change text-muted" style={{ fontSize: 11 }}>
                Mục tiêu: {caloTarget} kcal
              </div>
            </>
          )}
        </div>
        <div className="stat-card">
          <div className="stat-icon stat-icon-red"><Icon name="strength" /></div>
          <div className="stat-value">{sessions.length}</div>
          <div className="stat-label">Buổi tập gần đây</div>
        </div>
      </div>

      {/* Today's workout + quick actions */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        {/* Today */}
        <div className="card" style={{ gridColumn: '1 / -1' }}>
          <div className="section-header">
            <h2 className="section-title">
              📅 Hôm nay — {todayDay ? DAY_LABELS[todayDay.dayOfWeek] : 'Nghỉ ngơi'}
            </h2>
            <Link to={plan ? '/workout' : '/goal-setup'} className="btn btn-primary btn-sm">
              {plan ? 'Bắt đầu tập' : 'Thiết lập mục tiêu'}
            </Link>
          </div>

          {todayDay ? (
            <div>
              {todayDay.exercises.map((ex, idx) => (
                <div key={ex.id} className="row-item">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <span
                      style={{
                        width: 28,
                        height: 28,
                        borderRadius: '50%',
                        background: 'var(--mid-dark)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: 12,
                        fontWeight: 700,
                        flexShrink: 0,
                        color: 'var(--text-secondary)',
                      }}
                    >
                      {idx + 1}
                    </span>
                    <span style={{ fontWeight: 500 }}>{ex.exerciseName}</span>
                  </div>
                  <span className="text-secondary text-sm">
                    {ex.targetSets} × {ex.targetReps} · nghỉ {ex.restTimeSeconds}s
                  </span>
                </div>
              ))}
              <Link to="/plan" style={{ color: 'var(--green)', fontSize: 13, display: 'block', marginTop: 12 }}>
                Xem toàn bộ lộ trình →
              </Link>
            </div>
          ) : (
            <div className="empty-state" style={{ padding: '24px 0' }}>
              <div className="empty-state-icon" style={{ fontSize: 32 }}>🏖️</div>
              <p className="empty-state-text">
                {plan ? 'Hôm nay không có bài tập trong lộ trình — hãy nghỉ ngơi!' : 'Chưa có lộ trình — hãy thiết lập mục tiêu.'}
              </p>
              {!plan && (
                <Link to="/goal-setup" style={{ color: 'var(--green)', fontSize: 13, display: 'inline-block', marginTop: 10 }}>
                  Thiết lập mục tiêu →
                </Link>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Recent sessions */}
      <div className="card">
        <div className="section-header">
          <h2 className="section-title">⏱ Buổi tập gần đây</h2>
          <Link to="/history" className="section-link">Xem tất cả →</Link>
        </div>
        {sessions.length === 0 ? (
          <div className="empty-state" style={{ padding: '24px 0' }}>
            <div className="empty-state-icon" style={{ fontSize: 32 }}>🏋️</div>
            <p className="empty-state-text">Chưa có buổi tập nào. Bắt đầu ngay!</p>
          </div>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Ngày</th>
                <th>Hiệp</th>
                <th>Volume</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {sessions.map((s) => (
                <tr key={s.id}>
                  <td>{new Date(s.startTime).toLocaleDateString('vi-VN')}</td>
                  <td>{s.totalSets} hiệp</td>
                  <td className="text-secondary">{s.totalVolumeKg} kg</td>
                  <td>
                    <span className={`badge ${s.status === 'completed' ? 'badge-green' : s.status === 'active' ? 'badge-info' : 'badge-neutral'}`}>
                      {s.status === 'completed' ? 'Hoàn thành' : s.status === 'active' ? 'Đang tập' : s.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Quick actions */}
      <div>
        <h2 className="section-title" style={{ marginBottom: 12 }}>Truy cập nhanh</h2>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {[
            { to: '/workout',   label: 'Bắt đầu tập', icon: <Icon name="strength" />, variant: 'primary' as const },
            { to: '/nutrition', label: 'Dinh dưỡng',   icon: <Icon name="apple" />, variant: 'dark' as const },
            { to: '/stats',     label: 'Thống kê',     icon: <Icon name="chart" />, variant: 'dark' as const },
            { to: '/exercises', label: 'Thư viện',      icon: <Icon name="book" />, variant: 'dark' as const },
            { to: '/friends',   label: 'Bạn bè',       icon: <Icon name="users" />, variant: 'dark' as const },
            { to: '/leaderboard', label: 'Xếp hạng',   icon: <Icon name="trophy" />, variant: 'dark' as const },
          ].map(({ to, label, icon, variant }) => (
            <Link key={to} to={to} className={`btn btn-${variant} btn-sm`} style={{ textDecoration: 'none' }}>
              {icon}
              {label}
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}
