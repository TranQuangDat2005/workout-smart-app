import '@testing-library/jest-dom';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import WorkoutSession from './WorkoutSession';
import { trackingApi } from '../../services/trackingApi';
import type { WorkoutSession as WorkoutSessionModel, WorkoutSet } from '../../services/trackingApi';

jest.mock('../../services/trackingApi', () => ({
  trackingApi: {
    getActiveSession: jest.fn(),
    startSession: jest.fn(),
    recordSet: jest.fn(),
    incrementFocus: jest.fn(),
    deleteSet: jest.fn(),
    completeSession: jest.fn(),
  },
}));

const mockApi = trackingApi as jest.Mocked<typeof trackingApi>;

function buildSession(): WorkoutSessionModel {
  return {
    id: 1,
    status: 'active',
    startTime: new Date(Date.now() - 60_000).toISOString(),
    endTime: null,
    focusInterruptionsCount: 0,
    planId: null,
    exercises: [
      {
        id: 101,
        exerciseId: 201,
        exerciseName: 'Push-up',
        sortOrder: 0,
        targetSets: 3,
        targetReps: 10,
        restTimeSeconds: 90,
        sets: [
          { setNumber: 1, targetReps: 10, targetWeight: null, setType: 'normal', targetDurationSeconds: null },
          { setNumber: 2, targetReps: 10, targetWeight: 40, setType: 'normal', targetDurationSeconds: null },
        ],
        targetDurationSeconds: null,
        measureType: 'reps_weight',
        mediaUrl: null,
      },
      {
        id: 102,
        exerciseId: 202,
        exerciseName: 'Plank',
        sortOrder: 1,
        targetSets: 2,
        targetReps: 0,
        restTimeSeconds: 60,
        sets: [{ setNumber: 1, targetReps: 0, targetWeight: null, setType: 'normal', targetDurationSeconds: 60 }],
        targetDurationSeconds: 60,
        measureType: 'duration',
        mediaUrl: null,
      },
    ],
    sets: [],
  };
}

function savedSet(id: number, overrides: Partial<WorkoutSet> = {}): WorkoutSet {
  return {
    id,
    sessionId: 1,
    exerciseId: 201,
    setNumber: 1,
    repsCompleted: 10,
    weightUsed: null,
    restTimeSeconds: 90,
    sessionExerciseId: 101,
    setType: 'normal',
    durationSeconds: null,
    ...overrides,
  };
}

describe('WorkoutSession — màn tập trung tối giản (016)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    localStorage.clear();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('hiển thị đúng 4 khối: tên bài + thời lượng → GIF → Hiệp X - mục tiêu → nút HOÀN THÀNH', async () => {
    mockApi.getActiveSession.mockResolvedValue(buildSession());

    render(<WorkoutSession hasSchedule />);

    expect((await screen.findAllByText('Push-up')).length).toBeGreaterThan(0);
    expect(screen.getByText('Hiệp 1')).toBeInTheDocument();
    expect(screen.getByText('- 10 reps')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /HOÀN THÀNH/ })).toBeInTheDocument();
    // thời lượng từ startTime (60 giây trước) — không reset về 00:00
    expect(screen.getByText('01:00')).toBeInTheDocument();
    expect(screen.getByText('Tiếp theo: Plank')).toBeInTheDocument();
  });

  it('HOÀN THÀNH ghi hiệp theo target và có double-tap guard', async () => {
    mockApi.getActiveSession.mockResolvedValue(buildSession());
    let resolveRecord!: (value: WorkoutSet) => void;
    mockApi.recordSet.mockImplementation(
      () => new Promise<WorkoutSet>((resolve) => (resolveRecord = resolve)),
    );

    render(<WorkoutSession hasSchedule />);
    const button = await screen.findByRole('button', { name: /HOÀN THÀNH/ });

    fireEvent.click(button);
    fireEvent.click(button);
    fireEvent.click(button);

    expect(mockApi.recordSet).toHaveBeenCalledTimes(1);
    expect(mockApi.recordSet).toHaveBeenCalledWith(1, {
      exerciseId: 201,
      sessionExerciseId: 101,
      setNumber: 1,
      repsCompleted: 10,
      weightUsed: undefined,
      restTimeSeconds: 90,
      setType: 'normal',
      durationSeconds: undefined,
    });

    await act(async () => resolveRecord(savedSet(7)));
    await waitFor(() => expect(screen.getByText(/Đã lưu hiệp 1/)).toBeInTheDocument());
    // màn nghỉ lấy restTimeSeconds từ DB (90 giây)
    expect(screen.getByRole('timer', { name: 'thời gian nghỉ' })).toHaveTextContent('01:30');
  });

  it('hiệp drop-set không mở màn nghỉ', async () => {
    const session = buildSession();
    session.exercises[0].sets![0].setType = 'drop_set';
    mockApi.getActiveSession.mockResolvedValue(session);
    mockApi.recordSet.mockResolvedValue(savedSet(8, { setType: 'drop_set', restTimeSeconds: 0 }));

    render(<WorkoutSession hasSchedule />);
    fireEvent.click(await screen.findByRole('button', { name: /HOÀN THÀNH/ }));

    await waitFor(() => expect(mockApi.recordSet).toHaveBeenCalledTimes(1));
    expect(screen.queryByRole('timer', { name: 'thời gian nghỉ' })).not.toBeInTheDocument();
  });

  it('bài duration tự ghi khi đếm về 0 rồi chuyển màn nghỉ', async () => {
    mockApi.getActiveSession.mockResolvedValue(buildSession());
    mockApi.recordSet.mockResolvedValue(savedSet(9, { durationSeconds: 60, repsCompleted: null }));

    render(<WorkoutSession hasSchedule />);
    fireEvent.click(await screen.findByRole('button', { name: 'Plank' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Bắt đầu' }));

    act(() => {
      jest.advanceTimersByTime(60_000);
    });

    await waitFor(() =>
      expect(mockApi.recordSet).toHaveBeenCalledWith(1, expect.objectContaining({ durationSeconds: 60 })),
    );
    expect(await screen.findByRole('timer', { name: 'thời gian nghỉ' })).toHaveTextContent('01:00');
  });

  it('hoàn tác hiện trong 60 giây và xóa hiệp qua API', async () => {
    mockApi.getActiveSession.mockResolvedValue(buildSession());
    mockApi.recordSet.mockResolvedValue(savedSet(11));
    mockApi.deleteSet.mockResolvedValue(undefined);

    render(<WorkoutSession hasSchedule />);
    fireEvent.click(await screen.findByRole('button', { name: /HOÀN THÀNH/ }));
    await waitFor(() => expect(mockApi.recordSet).toHaveBeenCalledTimes(1));

    const undo = await screen.findByRole('button', { name: 'Hoàn tác hiệp vừa ghi' });
    fireEvent.click(undo);
    await waitFor(() => expect(mockApi.deleteSet).toHaveBeenCalledWith(1, 11));

    // ghi hiệp khác rồi đẩy đồng hồ quá 60 giây → nút ẩn
    const hoanThanh = await screen.findByRole('button', { name: /HOÀN THÀNH/ });
    mockApi.recordSet.mockResolvedValue(savedSet(12));
    fireEvent.click(hoanThanh);
    await waitFor(() => expect(mockApi.recordSet).toHaveBeenCalledTimes(2));
    act(() => {
      jest.advanceTimersByTime(61_000);
    });
    expect(screen.queryByRole('button', { name: 'Hoàn tác hiệp vừa ghi' })).not.toBeInTheDocument();
  });

  it('mini editor mở khi chạm dòng mục tiêu và giá trị sửa được dùng khi ghi', async () => {
    mockApi.getActiveSession.mockResolvedValue(buildSession());
    mockApi.recordSet.mockResolvedValue(savedSet(13));

    render(<WorkoutSession hasSchedule />);
    const setInfo = await screen.findByText('Hiệp 1');
    fireEvent.click(setInfo);

    const repsInput = screen.getByLabelText('Số lần (reps)');
    fireEvent.change(repsInput, { target: { value: '12' } });
    fireEvent.click(screen.getByRole('button', { name: /HOÀN THÀNH/ }));

    await waitFor(() =>
      expect(mockApi.recordSet).toHaveBeenCalledWith(1, expect.objectContaining({ repsCompleted: 12 })),
    );
  });

  it('tự động ghi nhận phân tâm khi rời tab, không có nút thủ công', async () => {
    mockApi.getActiveSession.mockResolvedValue(buildSession());
    mockApi.incrementFocus.mockResolvedValue({ ...buildSession(), focusInterruptionsCount: 1 });

    render(<WorkoutSession hasSchedule />);
    await screen.findAllByText('Push-up');
    expect(screen.queryByRole('button', { name: 'Báo phân tâm' })).not.toBeInTheDocument();

    Object.defineProperty(document, 'visibilityState', { value: 'hidden', configurable: true });
    fireEvent(document, new Event('visibilitychange'));

    await waitFor(() => expect(mockApi.incrementFocus).toHaveBeenCalledWith(1));
    expect(await screen.findByText('Phân tâm 1×')).toBeInTheDocument();
  });

  it('ô nhập nhạc hiển thị ở màn bắt đầu và khôi phục từ localStorage', async () => {
    localStorage.setItem('workoutMusicUrl', 'https://youtu.be/dQw4w9WgXcQ');
    mockApi.getActiveSession.mockRejectedValue({ response: { status: 404 } });

    render(<WorkoutSession hasSchedule />);

    const input = await screen.findByLabelText('Nhạc luyện tập (URL YouTube/Spotify/mp3 — tùy chọn)');
    expect(input).toHaveValue('https://youtu.be/dQw4w9WgXcQ');
  });

  it('URL nhạc không hợp lệ → lỗi nhẹ nhưng buổi tập vẫn bắt đầu', async () => {
    mockApi.getActiveSession.mockRejectedValue({ response: { status: 404 } });
    mockApi.startSession.mockResolvedValue(buildSession());

    render(<WorkoutSession hasSchedule />);
    const input = await screen.findByLabelText('Nhạc luyện tập (URL YouTube/Spotify/mp3 — tùy chọn)');
    fireEvent.change(input, { target: { value: 'abc' } });
    fireEvent.click(screen.getByRole('button', { name: 'Bắt đầu buổi tập' }));

    await waitFor(() => expect(mockApi.startSession).toHaveBeenCalledTimes(1));
    expect(await screen.findByText('URL nhạc không hợp lệ — đã bỏ qua nhạc')).toBeInTheDocument();
    expect(screen.queryByTitle('Spotify player')).not.toBeInTheDocument();
  });
});
