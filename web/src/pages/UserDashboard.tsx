import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Icon from '../components/Icon';
import { planApi } from '../services/planApi';
import type { WorkoutPlan } from '../services/planApi';
import { profileApi } from '../services/profileApi';
import type { Profile } from '../services/profileApi';
import { statsApi } from '../services/statsApi';
import type { StatsDashboard } from '../services/statsApi';
import { nutritionApi } from '../services/nutritionApi';
import type { NutritionSummary } from '../services/nutritionApi';
import { socialApi } from '../services/socialApi';
import type { FeedItem } from '../services/socialApi';
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

/** FR-005/FR-006 (018): label tiếng Việt cho từng loại sự kiện thành tích. */
function feedLabel(item: FeedItem): string {
  switch (item.actionType) {
    case 'streak_milestone': {
      const weeks = /"streakWeeks":(\d+)/.exec(item.detailsJson ?? '');
      return weeks ? `đạt chuỗi ${weeks[1]} tuần` : 'đạt mốc chuỗi mới';
    }
    case 'new_pr': {
      const kg = /"volumeKg":([0-9.]+)/.exec(item.detailsJson ?? '');
      return kg ? `phá kỷ lục: tổng khối lượng ${kg[1]} kg` : 'phá kỷ lục cá nhân';
    }
    case 'friendship_created':
      return 'kết bạn mới';
    default:
      return item.actionType;
  }
}

/** Dashboard người dùng — tổng quan mục tiêu, streak, hôm nay tập gì, dinh dưỡng. */
export default function UserDashboard() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [plan, setPlan] = useState<WorkoutPlan | null>(null);
  const [stats, setStats] = useState<StatsDashboard | null>(null);
  const [summary, setSummary] = useState<NutritionSummary | null>(null);
  const [feedItems, setFeedItems] = useState<FeedItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    void Promise.all([
      profileApi.getProfile().then(setProfile).catch(() => {}),
      planApi.getActivePlan().then(setPlan).catch(() => setPlan(null)),
      statsApi.dashboard().then(setStats).catch(() => {}),
      nutritionApi.getSummary(todayLocalISO()).then(setSummary).catch(() => {}),
    ]).finally(() => setLoading(false));
    // Feed bạn bè: tải ngay + polling 60s (SC-002: sự kiện mới ≤ 1 phút)
    const loadFeed = () => {
      socialApi.feed().then(setFeedItems).catch(() => {});
    };
    loadFeed();
    const timer = setInterval(loadFeed, 60_000);
    return () => clearInterval(timer);
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
          <span style={{ color: 'var(--green)' }}>{profile?.displayName ?? 'bạn'}</span>! <Icon name="wave" size={24} style={{ verticalAlign: '-4px', marginLeft: 4 }} />
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
      </div>

      {/* Hoạt động bạn bè (FR-005/006) */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        <div className="card" style={{ gridColumn: '1 / -1' }}>
          <h2 className="section-title">
            <Icon name="users" size={17} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Hoạt động bạn bè
          </h2>
          {feedItems.length === 0 ? (
            <p className="text-secondary text-sm" style={{ padding: '12px 0' }}>
              Chưa có hoạt động mới từ bạn bè trong 7 ngày qua.
            </p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 8 }}>
              {feedItems.slice(0, 5).map((item) => (
                <div key={item.id} className="row-item" style={{ justifyContent: 'flex-start' }}>
                  <span className="fw-600" style={{ fontSize: 14 }}>{item.displayName ?? 'Bạn của bạn'}</span>
                  <span className="text-secondary text-sm" style={{ marginLeft: 8 }}>{feedLabel(item)}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Today's workout + quick actions */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        {/* Today */}
        <div className="card" style={{ gridColumn: '1 / -1' }}>
          <div className="section-header">
            <h2 className="section-title">
              <Icon name="calendar" size={17} style={{ verticalAlign: '-2px', marginRight: 6 }} /> Hôm nay — {todayDay ? DAY_LABELS[todayDay.dayOfWeek] : 'Nghỉ ngơi'}
            </h2>
            <Link to="/training" className="btn btn-primary btn-sm">
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
              <Link to="/training" style={{ color: 'var(--green)', fontSize: 13, display: 'block', marginTop: 12 }}>
                Xem toàn bộ lộ trình →
              </Link>
            </div>
          ) : (
            <div className="empty-state" style={{ padding: '24px 0' }}>
              <div className="empty-state-icon"><Icon name="sun" size={42} /></div>
              <p className="empty-state-text">
                {plan ? 'Hôm nay không có bài tập trong lộ trình — hãy nghỉ ngơi!' : 'Chưa có lộ trình — hãy thiết lập mục tiêu.'}
              </p>
              {!plan && (
                <Link to="/training" style={{ color: 'var(--green)', fontSize: 13, display: 'inline-block', marginTop: 10 }}>
                  Thiết lập mục tiêu →
                </Link>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
