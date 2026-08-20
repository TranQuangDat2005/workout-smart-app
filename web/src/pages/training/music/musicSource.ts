/** Phân loại URL nhạc luyện tập — 017 FR-002. */
export type MusicSourceType = 'youtube' | 'spotify' | 'audio';

export interface MusicSource {
  type: MusicSourceType;
  url: string;
  /** YouTube: video id; Spotify: "track/xxx", "album/xxx"…; mp3: không có */
  id?: string;
}

const AUDIO_EXT = /\.(mp3|ogg|m4a|wav|aac)(\?.*)?$/i;
const YOUTUBE_RE =
  /(?:youtube\.com\/(?:watch\?[^#]*v=|shorts\/|embed\/)|youtu\.be\/)([\w-]{11})/i;
const SPOTIFY_RE =
  /(?:open\.spotify\.com\/(?:intl-\w+\/)?(track|album|playlist|episode|show)\/|spotify:(track|album|playlist|episode|show):)([\w]+)/i;

/** Parse URL nhạc do User cung cấp → loại nguồn + id (nếu có); null nếu không hợp lệ. */
export function parseMusicSource(raw: string): MusicSource | null {
  const url = raw.trim();
  if (!url) return null;

  if (AUDIO_EXT.test(url)) {
    return { type: 'audio', url };
  }

  const yt = url.match(YOUTUBE_RE);
  if (yt) {
    return { type: 'youtube', url, id: yt[1] };
  }

  const sp = url.match(SPOTIFY_RE);
  if (sp) {
    const kind = (sp[1] || sp[2]).toLowerCase();
    return { type: 'spotify', url, id: `${kind}/${sp[3]}` };
  }

  return null;
}
