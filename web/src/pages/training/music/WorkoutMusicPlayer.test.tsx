import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import WorkoutMusicPlayer from './WorkoutMusicPlayer';
import { musicEngine } from './musicEngine';
import type { MusicSource } from './musicSource';

describe('WorkoutMusicPlayer (017)', () => {
  afterEach(() => {
    // Engine là singleton — reset sau mỗi test để không rò rỉ player giữa các test.
    musicEngine.stop();
  });
  it('audio: hiển thị nút phát + volume + seek', () => {
    render(
      <WorkoutMusicPlayer
        source={{ type: 'audio', url: 'https://cdn.example.com/song.mp3' }}
        onError={jest.fn()}
      />,
    );

    expect(screen.getByRole('button', { name: /Phát/ })).toBeInTheDocument();
    expect(screen.getByLabelText('Âm lượng')).toBeInTheDocument();
    expect(screen.getByLabelText('Tiến trình phát')).toBeInTheDocument();
  });

  it('spotify: nhúng iframe embed đúng path', () => {
    const source: MusicSource = {
      type: 'spotify',
      url: 'https://open.spotify.com/track/abc',
      id: 'track/abc',
    };
    render(<WorkoutMusicPlayer source={source} onError={jest.fn()} />);

    expect(screen.getByTitle('Spotify player')).toHaveAttribute(
      'src',
      'https://open.spotify.com/embed/track/abc',
    );
  });

  it('youtube: hiển thị vùng player và nút phát chờ sẵn sàng', () => {
    render(
      <WorkoutMusicPlayer
        source={{ type: 'youtube', url: 'https://youtu.be/dQw4w9WgXcQ', id: 'dQw4w9WgXcQ' }}
        onError={jest.fn()}
      />,
    );

    expect(screen.getByRole('button', { name: /Phát/ })).toBeDisabled();
    expect(document.getElementById('yt-player-dQw4w9WgXcQ')).toBeInTheDocument();
  });

  it('re-render với object source mới cùng key KHÔNG tạo lại player (idempotent)', () => {
    const onError = jest.fn();
    const { rerender } = render(
      <WorkoutMusicPlayer
        source={{ type: 'audio', url: 'https://cdn.example.com/song.mp3' }}
        onError={onError}
      />,
    );
    const stopSpy = jest.spyOn(musicEngine, 'stop');

    // Object mới nhưng cùng url → effect không chạy lại → engine không stop/restart.
    rerender(
      <WorkoutMusicPlayer
        source={{ type: 'audio', url: 'https://cdn.example.com/song.mp3' }}
        onError={onError}
      />,
    );

    expect(stopSpy).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: /Phát/ })).toBeInTheDocument();
  });
});
