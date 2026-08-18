import { useEffect, useState } from 'react';
import type { CSSProperties } from 'react';
import { Link } from 'react-router-dom';
import { planApi } from '../services/planApi';
import type { WorkoutPlan } from '../services/planApi';
import { profileApi } from '../services/profileApi';
import type { Profile, WorkoutSessionItem } from '../services/profileApi';
import { statsApi } from '../services/statsApi';
import type { StatsDashboard } from '../services/statsApi';
import { nutritionApi } from '../services/nutritionApi';
import type { NutritionSummary } from '../services/nutritionApi';

const DAY_LABELS = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];

const cardStyle: CSSProperties = {
  background: 'var(--dark-surface)',
  borderRadius: 12,
  padding: 20,
  flex: '1 1 150px',
  minWidth: 150,
  textAlign: 'center',
};

const actionStyle: CSSProperties = {
  background: 'var(--dark-surface)',
  color: 'var(--text-base)',
  borderRadius: 9999,
  padding: '12px 20px',
  fontSize: 14,
  fontWeight: 700,
  textDecoration: 'none',
  border: '1px solid var(--mid-dark)',
};

/** Dashboard người dùng — tổng quan mục tiêu, streak, hôm nay tập gì, bữa ăn, buổi gần đây. */
export default function UserDashboard() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [plan, setPlan] = useState<WorkoutPlan | null>(null);
  const [stats, setStats] = useState<StatsDashboard | null>(null);
  const [sessions, setSessions] = useState<WorkoutSessionItem[]>([]);
  const [summary, setSummary] = useState<NutritionSummary | null>(null);

  useEffect(() => {
    profileApi.getProfile().then(setProfile).catch(() => {});
    planApi.getActivePlan().then(setPlan).catch(() => setPlan(null));
    statsApi.dashboard().then(setStats).catch(() => {});
    profileApi.getSessions(0, 5).then((r) => setSessions(r.content)).catch(() => {});
    const today = new Date().toISOString().slice(0, 10);
    nutritionApi.getSummary(today).then(setSummary).catch(() => {});
  }, []);

  const today = new Date().getDay();
  const todayDay = plan?.days.find((d) => d.dayOfWeek === today);

  return (
    <div style={{ width: '100%', maxWidth: 960, display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div>
        <h1 style={{ fontSize: 26, fontWeight: 700 }}>
          Xin chào, {profile?.displayName ?? 'bạn'}!
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>
          Mục tiêu: {profile?.goalType ?? '—'} · Trình độ: {profile?.fitnessLevel ?? '—'} · Cân nặng:{' '}
          {profile?.weightKg != null ? `${profile.weightKg} kg` : '—'}
        </p>
      </div>

      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        <div style={cardStyle}>
          <div style={{ fontSize: 32, fontWeight: 700, color: 'var(--green)' }}>
            {stats?.streak.currentStreakWeeks ?? 0}
          </div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: 1 }}>
            Tuần streak
          </div>
        </div>
        <div style={cardStyle}>
          <div style={{ fontSize: 32, fontWeight: 700 }}>{stats?.planCompletion.completionPct ?? 0}%</div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: 1 }}>
            Hoàn thành lộ trình
          </div>
        </div>
        <div style={cardStyle}>
          <div style={{ fontSize: 32, fontWeight: 700 }}>{summary?.totalCalories ?? 0}</div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: 1 }}>
            Calo hôm nay / {summary?.targetCalories ?? '—'}
          </div>
        </div>
        <div style={cardStyle}>
          <div style={{ fontSize: 32, fontWeight: 700 }}>{sessions.length}</div>
          <div style={{ fontSize: 12, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: 1 }}>
            Buổi tập gần đây
          </div>
        </div>
      </div>

      <div style={{ background: 'var(--dark-surface)', borderRadius: 12, padding: 20 }}>
        <h2 style={{ fontSize: 18, fontWeight: 700 }}>
          Hôm nay — {todayDay ? DAY_LABELS[todayDay.dayOfWeek] : 'Nghỉ ngơi'}
        </h2>
        {todayDay ? (
          todayDay.exercises.map((ex) => (
            <div
              key={ex.id}
              style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--mid-dark)' }}
            >
              <span>{ex.exerciseName}</span>
              <span style={{ color: 'var(--text-secondary)' }}>
                {ex.targetSets} × {ex.targetReps} · nghỉ {ex.restTimeSeconds}s
              </span>
            </div>
          ))
        ) : (
          <p style={{ color: 'var(--text-secondary)' }}>
            {plan ? 'Hôm nay không có bài tập trong lộ trình.' : 'Chưa có lộ trình — hãy thiết lập mục tiêu.'}
          </p>
        )}
        <Link to={plan ? '/plan' : '/goal-setup'} style={{ color: 'var(--green)', fontSize: 14 }}>
          {plan ? 'Xem toàn bộ lộ trình →' : 'Thiết lập mục tiêu →'}
        </Link>
      </div>

      <div style={{ background: 'var(--dark-surface)', borderRadius: 12, padding: 20 }}>
        <h2 style={{ fontSize: 18, fontWeight: 700 }}>Buổi tập gần đây</h2>
        {sessions.length === 0 ? (
          <p style={{ color: 'var(--text-secondary)' }}>Chưa có buổi tập nào. Bắt đầu ngay!</p>
        ) : (
          sessions.map((s) => (
            <div
              key={s.id}
              style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--mid-dark)' }}
            >
              <span>{new Date(s.startTime).toLocaleDateString('vi-VN')}</span>
              <span style={{ color: 'var(--text-secondary)' }}>
                {s.totalSets} hiệp · {s.totalVolumeKg} kg · {s.status}
              </span>
            </div>
          ))
        )}
        <Link to="/history" style={{ color: 'var(--green)', fontSize: 14 }}>
          Xem lịch sử đầy đủ →
        </Link>
      </div>

      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        <Link to="/workout" style={actionStyle}>▶ Bắt đầu tập</Link>
        <Link to="/nutrition" style={actionStyle}>🍽 Dinh dưỡng</Link>
        <Link to="/stats" style={actionStyle}>📊 Thống kê</Link>
        <Link to="/exercises" style={actionStyle}>🔍 Thư viện bài tập</Link>
        <Link to="/friends" style={actionStyle}>👥 Bạn bè</Link>
        <Link to="/leaderboard" style={actionStyle}>🏆 Bảng xếp hạng</Link>
      </div>
    </div>
  );
}
