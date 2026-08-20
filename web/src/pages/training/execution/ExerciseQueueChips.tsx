import Icon from '../../../components/Icon';

export interface QueueChip {
  id: number;
  name: string;
  done: boolean;
  current: boolean;
}

interface ExerciseQueueChipsProps {
  chips: QueueChip[];
  onSelect: (id: number) => void;
}

/** Hàng đợi bài tập (016 FR-008): chip xong/đang/tới, chạm để chuyển bài; quá dài thì cuộn ngang. */
export default function ExerciseQueueChips({ chips, onSelect }: ExerciseQueueChipsProps) {
  if (chips.length === 0) return null;
  return (
    <div
      style={{
        display: 'flex',
        gap: 6,
        flexWrap: 'nowrap',
        overflowX: 'auto',
        paddingBottom: 4,
      }}
    >
      {chips.map((chip) => (
        <button
          key={chip.id}
          type="button"
          onClick={() => onSelect(chip.id)}
          className={`btn btn-sm ${chip.current ? 'btn-primary' : 'btn-dark'}`}
          style={{
            opacity: chip.done && !chip.current ? 0.55 : 1,
            flexShrink: 0,
            whiteSpace: 'nowrap',
          }}
        >
          {chip.done && !chip.current && (
            <Icon name="check" size={12} style={{ verticalAlign: '-2px', marginRight: 4 }} />
          )}
          {chip.name}
        </button>
      ))}
    </div>
  );
}
