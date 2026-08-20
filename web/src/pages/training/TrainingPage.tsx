import { useCallback, useEffect, useState } from 'react';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import Spinner from '../../components/Spinner';
import { planApi } from '../../services/planApi';
import type { WorkoutPlan } from '../../services/planApi';
import GoalSetupForm from './GoalSetupForm';
import PlanEditor from './PlanEditor';
import WorkoutSession from './WorkoutSession';

const GOAL_LABELS: Record<string, string> = {
  weight_loss: 'Giảm cân', muscle_gain: 'Tăng cơ', endurance: 'Sức bền',
};
const LEVEL_LABELS: Record<string, string> = {
  beginner: 'Mới bắt đầu', intermediate: 'Trung bình', advanced: 'Nâng cao',
};

type TabKey = 'today' | 'plan';

export default function TrainingPage() {
  const [plan, setPlan] = useState<WorkoutPlan | null>(null);
  const [loading, setLoading] = useState(true);
  const [showSetup, setShowSetup] = useState(false);
  const [tab, setTab] = useState<TabKey>('today');

  const loadPlan = useCallback(() => {
    setLoading(true);
    planApi
      .getActivePlan()
      .then((p) => {
        setPlan(p);
        setShowSetup(false);
        setTab('today');
      })
      .catch(() => {
        setPlan(null);
        setShowSetup(true);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadPlan();
  }, [loadPlan]);

  if (loading) {
    return (
      <div className="page-container" style={{ display: 'flex', justifyContent: 'center', paddingTop: 80 }}>
        <Spinner />
      </div>
    );
  }

  const goalLabel = plan ? (GOAL_LABELS[plan.goalType] ?? plan.goalType) : '';
  const levelLabel = plan ? (LEVEL_LABELS[plan.fitnessLevel] ?? plan.fitnessLevel) : '';
  const today = new Date().getDay();
  const todayHasSchedule = !!plan?.days.find((d) => d.dayOfWeek === today)?.exercises.length;

  return (
    <div className="page-container" style={{ maxWidth: 800, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="page-header">
        <div>
          <h1><Icon name="dumbbell" size={22} style={{ verticalAlign: '-3px', marginRight: 8 }} /> Luyện tập</h1>
          {plan && (
            <p className="text-secondary text-sm" style={{ marginTop: 4 }}>
              {plan.name}
              {goalLabel && ` · ${goalLabel}`}
              {levelLabel && ` · ${levelLabel}`}
            </p>
          )}
        </div>
        {plan && (
          <Button
            variant="outlined"
            size="sm"
            onClick={() => setShowSetup((v) => !v)}
          >
            {showSetup ? '← Quay lại' : 'Tạo lịch tập tự động'}
          </Button>
        )}
      </div>

      {!plan || showSetup ? (
        <>
          <div className="notice notice-info">
            Chọn mục tiêu, trình độ và dụng cụ — hệ thống Rule Engine sẽ tự tạo lộ trình phù hợp.
          </div>
          <GoalSetupForm onDone={loadPlan} />
        </>
      ) : (
        <>
          <div style={{ display: 'flex', gap: 8 }}>
            {([
              { key: 'today', label: 'Hôm nay' },
              { key: 'plan', label: 'Lịch tập' },
            ] as { key: TabKey; label: string }[]).map((t) => (
              <button
                key={t.key}
                type="button"
                className={`btn btn-sm ${tab === t.key ? 'btn-primary' : 'btn-dark'}`}
                onClick={() => setTab(t.key)}
              >
                {t.label}
              </button>
            ))}
          </div>

          {/* Giữ cả 2 tab mounted để không mất trạng thái buổi tập đang chạy khi chuyển tab */}
          <div style={{ display: tab === 'today' ? 'block' : 'none' }}>
            <WorkoutSession hasSchedule={todayHasSchedule} />
          </div>
          <div style={{ display: tab === 'plan' ? 'block' : 'none' }}>
            <PlanEditor plan={plan} onPlanChange={setPlan} visible={tab === 'plan'} />
          </div>
        </>
      )}
    </div>
  );
}
