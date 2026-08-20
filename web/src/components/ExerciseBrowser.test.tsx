import '@testing-library/jest-dom';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import ExerciseBrowser from './ExerciseBrowser';
import { planApi } from '../services/planApi';
import type { ExerciseDetail } from '../services/planApi';

jest.mock('../services/planApi', () => ({
  mediaUrl: (path: string | null | undefined) => (path ? `/media/${path}` : null),
  planApi: {
    searchExercises: jest.fn(),
    getExercise: jest.fn(),
  },
}));

const sample: ExerciseDetail = {
  id: 3220,
  name: 'astride jumps (male)',
  category: 'cardio',
  bodyPart: 'cardio',
  equipment: 'body_weight',
  target: null,
  muscleGroup: 'legs',
  image: 'images/3220.jpg',
  gifUrl: 'videos/3220.gif',
  instructions: 'Jump explosively upwards.',
  source: 'system',
};

const searchMock = planApi.searchExercises as jest.Mock;
const detailMock = planApi.getExercise as jest.Mock;

beforeEach(() => {
  searchMock.mockResolvedValue({ content: [sample], totalElements: 1, totalPages: 1 });
  detailMock.mockResolvedValue(sample);
});

describe('ExerciseBrowser', () => {
  it('shows Category and equipment chips like the library', async () => {
    render(<ExerciseBrowser />);
    expect(screen.getByText('Category')).toBeInTheDocument();
    expect(screen.getByText('Dụng cụ')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ngực' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cân nặng cơ thể' })).toBeInTheDocument();
    expect(await screen.findByText('astride jumps (male)')).toBeInTheDocument();
  });

  it('filters by category chip', async () => {
    render(<ExerciseBrowser />);
    await screen.findByText('astride jumps (male)');
    fireEvent.click(screen.getByRole('button', { name: 'Ngực' }));
    await waitFor(() => {
      expect(searchMock).toHaveBeenCalledWith(expect.objectContaining({ category: ['chest'] }));
    });
  });

  it('previews GIF without adding until confirm', async () => {
    const onPick = jest.fn();
    render(<ExerciseBrowser onPick={onPick} />);
    fireEvent.click(await screen.findByText('astride jumps (male)'));
    expect(await screen.findByAltText('astride jumps (male)')).toHaveAttribute('src', '/media/videos/3220.gif');
    expect(screen.getByText('Jump explosively upwards.')).toBeInTheDocument();
    expect(onPick).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'Thêm vào ngày này' }));
    await waitFor(() => expect(onPick).toHaveBeenCalledWith(expect.objectContaining({ id: 3220 })));
  });
});
