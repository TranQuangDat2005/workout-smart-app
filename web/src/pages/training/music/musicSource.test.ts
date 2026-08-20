import { parseMusicSource } from './musicSource';

describe('parseMusicSource (017)', () => {
  it('nhận diện file audio trực tiếp kể cả có query', () => {
    expect(parseMusicSource('https://cdn.example.com/song.mp3')?.type).toBe('audio');
    expect(parseMusicSource('https://cdn.example.com/song.ogg?x=1')?.type).toBe('audio');
    expect(parseMusicSource('https://cdn.example.com/song.m4a')?.type).toBe('audio');
    expect(parseMusicSource('https://cdn.example.com/song.wav')?.type).toBe('audio');
  });

  it('nhận diện YouTube watch/shorts/youtu.be/embed', () => {
    const cases: [string, string][] = [
      ['https://www.youtube.com/watch?v=dQw4w9WgXcQ', 'dQw4w9WgXcQ'],
      ['https://www.youtube.com/watch?t=10&v=dQw4w9WgXcQ', 'dQw4w9WgXcQ'],
      ['https://www.youtube.com/shorts/dQw4w9WgXcQ', 'dQw4w9WgXcQ'],
      ['https://youtu.be/dQw4w9WgXcQ?t=5', 'dQw4w9WgXcQ'],
      ['https://www.youtube.com/embed/dQw4w9WgXcQ', 'dQw4w9WgXcQ'],
    ];
    for (const [url, id] of cases) {
      const source = parseMusicSource(url);
      expect(source?.type).toBe('youtube');
      expect(source?.id).toBe(id);
    }
  });

  it('nhận diện Spotify web link lẫn URI', () => {
    expect(parseMusicSource('https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT')).toMatchObject({
      type: 'spotify',
      id: 'track/4cOdK2wGLETKBW3PvgPWqT',
    });
    expect(parseMusicSource('https://open.spotify.com/intl-vi/album/abcdef')).toMatchObject({
      type: 'spotify',
      id: 'album/abcdef',
    });
    expect(parseMusicSource('spotify:playlist:37i9dQZF1DXcBWIGoYBM5M')).toMatchObject({
      type: 'spotify',
      id: 'playlist/37i9dQZF1DXcBWIGoYBM5M',
    });
  });

  it('trả null cho URL không hợp lệ', () => {
    expect(parseMusicSource('')).toBeNull();
    expect(parseMusicSource('abc')).toBeNull();
    expect(parseMusicSource('https://example.com')).toBeNull();
    expect(parseMusicSource('https://www.youtube.com/watch?v=short')).toBeNull();
  });
});
