import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Button from '../../components/Button';
import { planApi } from '../../services/planApi';
import type { WorkoutPlan } from '../../services/planApi';

const DAY_LABELS = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

export default function PlanPage() {
  const [plan, setPlan] = useState<WorkoutPlan | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    planApi
      .getActivePlan()
      .then(setPlan)
      .catch(() => setError('Chưa có lộ trình tập. Hãy thiết lập mục tiêu.'));
  }, []);

  if (error && !plan) {
    return (
      <div style={{ minHeight: '100vh', background: 'var(--near-black)', padding: 32, textAlign: 'center' }}>
        <p style={{ color: 'var(--text-secondary)' }}>{error}</p>
        <Link to="/goal-setup"><Button>Tạo lộ trình</Button></Link>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', background: 'var(--near-black)', padding: 32 }}>
      <h1 style={{ fontSize: 24, fontWeight: 700 }}>{plan?.name ?? 'Lộ trình tập'}</h1>
      <p style={{ color: 'var(--text-secondary)' }}>Mục tiêu: {plan?.goalType} · Trình độ: {plan?.fitnessLevel}</p>
      {plan?.days.map((day) => (
        <div key={day.id} style={{ background: 'var(--dark-surface)', borderRadius: 8, padding: 16, marginTop: 16 }}>
          <h2 style={{ fontSize: 18, fontWeight: 700 }}>{DAY_LABELS[day.dayOfWeek]}</h2>
          {day.exercises.map((ex) => (
            <div key={ex.id} style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid var(--mid-dark)' }}>
              <span>{ex.exerciseName}</span>
              <span style={{ color: 'var(--text-secondary)' }}>{ex.targetSets} hiệp × {ex.targetReps} reps · nghỉ {ex.restTimeSeconds}s</span>
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}
