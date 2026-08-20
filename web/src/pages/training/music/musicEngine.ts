import type { MusicSource } from './musicSource';

/**
 * MusicEngine — luồng phát nhạc TÁCH KHỎI React lifecycle (017).
 * Player (Audio/YouTube) sống ở đây, component chỉ subscribe state.
 * Re-render của React KHÔNG bao giờ tạo lại player → không reload iframe, không gián đoạn phát.
 */

export interface MusicState {
  type: MusicSource['type'];
  playing: boolean;
  ready: boolean;
  volume: number;
  currentTime: number;
  duration: number;
}

type Listener = (state: MusicState) => void;

/** Interface tối thiểu của YouTube IFrame Player API mà app sử dụng. */
interface YTPlayerLike {
  playVideo: () => void;
  pauseVideo: () => void;
  seekTo: (seconds: number, allowSeekAhead: boolean) => void;
  setVolume: (volume: number) => void;
  getCurrentTime: () => number;
  getDuration: () => number;
  destroy: () => void;
}

interface YTApiLike {
  Player: new (elementId: string, options: Record<string, unknown>) => YTPlayerLike;
}

let youtubeApiPromise: Promise<YTApiLike> | null = null;

/** Nạp script https://www.youtube.com/iframe_api đúng 1 lần cho toàn app. */
function loadYouTubeApi(): Promise<YTApiLike> {
  if (youtubeApiPromise) return youtubeApiPromise;
  youtubeApiPromise = new Promise((resolve) => {
    const w = window as unknown as { YT?: YTApiLike; onYouTubeIframeAPIReady?: () => void };
    if (w.YT?.Player) {
      resolve(w.YT);
      return;
    }
    const prev = w.onYouTubeIframeAPIReady;
    w.onYouTubeIframeAPIReady = () => {
      prev?.();
      resolve(w.YT as YTApiLike);
    };
    if (!document.querySelector('script[src="https://www.youtube.com/iframe_api"]')) {
      const script = document.createElement('script');
      script.src = 'https://www.youtube.com/iframe_api';
      document.head.appendChild(script);
    }
  });
  return youtubeApiPromise;
}

class MusicEngine {
  private state: MusicState = {
    type: 'audio',
    playing: false,
    ready: false,
    volume: 80,
    currentTime: 0,
    duration: 0,
  };
  private listeners = new Set<Listener>();
  private key: string | null = null;
  private onError: ((message: string) => void) | null = null;
  private audio: HTMLAudioElement | null = null;
  private ytPlayer: YTPlayerLike | null = null;
  private timer: number | null = null;

  subscribe = (listener: Listener): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  getSnapshot = (): MusicState => this.state;

  private setState(patch: Partial<MusicState>) {
    this.state = { ...this.state, ...patch };
    this.listeners.forEach((listener) => listener(this.state));
  }

  /** Bắt đầu nguồn nhạc. Idempotent: cùng key → giữ nguyên player đang chạy (không reload). */
  start(source: MusicSource, onError: (message: string) => void): void {
    this.onError = onError;
    const key = `${source.type}:${source.id ?? source.url}`;
    if (key === this.key) return;
    this.stop();
    this.key = key;
    if (source.type === 'audio') {
      this.startAudio(source.url);
    } else if (source.type === 'youtube' && source.id) {
      this.startYouTube(source.id);
    }
    // spotify: iframe embed tự quản lý — engine không cần giữ gì.
  }

  /** Dừng và hủy player (kết thúc buổi tập → component unmount gọi hàm này). */
  stop(): void {
    if (this.timer != null) {
      window.clearInterval(this.timer);
      this.timer = null;
    }
    try {
      this.audio?.pause();
    } catch {
      /* môi trường không implement pause (jsdom) */
    }
    this.audio = null;
    try {
      this.ytPlayer?.destroy();
    } catch {
      /* player chưa khởi tạo xong */
    }
    this.ytPlayer = null;
    this.key = null;
    this.setState({ playing: false, ready: false, currentTime: 0, duration: 0 });
  }

  toggle(): void {
    if (this.audio) {
      if (this.state.playing) {
        try {
          this.audio.pause();
        } catch {
          /* không implement pause */
        }
      } else {
        try {
          const maybePlay = this.audio.play();
          if (maybePlay && typeof maybePlay.catch === 'function') {
            maybePlay.catch(() => this.setState({ playing: false }));
          }
        } catch {
          /* autoplay bị chặn — user bấm lại */
        }
      }
      return;
    }
    if (this.ytPlayer) {
      try {
        if (this.state.playing) this.ytPlayer.pauseVideo();
        else this.ytPlayer.playVideo();
      } catch {
        /* player đang chuyển trạng thái */
      }
    }
  }

  setVolume(volume: number): void {
    this.setState({ volume });
    try {
      if (this.audio) this.audio.volume = volume / 100;
    } catch {
      /* noop */
    }
    try {
      this.ytPlayer?.setVolume(volume);
    } catch {
      /* noop */
    }
  }

  seekTo(seconds: number): void {
    this.setState({ currentTime: seconds });
    try {
      if (this.audio) this.audio.currentTime = seconds;
      else this.ytPlayer?.seekTo(seconds, true);
    } catch {
      /* noop */
    }
  }

  private startAudio(url: string): void {
    const AudioCtor = (window.Audio ?? window.HTMLAudioElement) as typeof Audio;
    const audio = new AudioCtor(url);
    this.audio = audio;
    audio.volume = this.state.volume / 100;
    audio.addEventListener('timeupdate', () =>
      this.setState({ currentTime: audio.currentTime, duration: audio.duration || 0 }),
    );
    audio.addEventListener('loadedmetadata', () => this.setState({ duration: audio.duration || 0 }));
    audio.addEventListener('play', () => this.setState({ playing: true }));
    audio.addEventListener('pause', () => this.setState({ playing: false }));
    audio.addEventListener('error', () => this.onError?.('Không phát được URL nhạc — kiểm tra lại link'));
    this.setState({ type: 'audio', ready: true, currentTime: 0, duration: 0 });
    try {
      const maybePlay = audio.play();
      if (maybePlay && typeof maybePlay.catch === 'function') {
        maybePlay.catch(() => this.setState({ playing: false }));
      }
    } catch {
      /* autoplay bị chặn — hiển thị nút phát */
    }
  }

  private startYouTube(videoId: string): void {
    this.setState({ type: 'youtube', ready: false, currentTime: 0, duration: 0 });
    void loadYouTubeApi().then((YT) => {
      // Nguồn đã đổi/dừng trong lúc tải API → không tạo player mồ côi.
      if (this.key !== `youtube:${videoId}`) return;
      // StrictMode double-mount có thể gọi 2 lần — không tạo đè player đang chạy.
      if (this.ytPlayer) return;
      const player = new YT.Player(`yt-player-${videoId}`, {
        videoId,
        // enablejsapi=1 là BẮT BUỘC để YouTube gửi onReady/onStateChange — thiếu thì nút điều khiển kẹt mãi.
        playerVars: { autoplay: 1, controls: 0, enablejsapi: 1 },
        events: {
          onReady: () => {
            if (this.ytPlayer !== player) return;
            player.setVolume(this.state.volume);
            this.setState({ ready: true });
            try {
              player.playVideo();
            } catch {
              this.setState({ playing: false });
            }
          },
          onStateChange: (event: { data: number }) => {
            if (event.data === 1) {
              this.setState({ playing: true });
              this.startPoll();
            } else if (event.data === 2 || event.data === 0) {
              this.setState({ playing: false });
            }
          },
        },
      });
      this.ytPlayer = player;
    });
  }

  private startPoll(): void {
    if (this.timer != null) return;
    this.timer = window.setInterval(() => {
      const player = this.ytPlayer;
      if (player) {
        this.setState({ currentTime: player.getCurrentTime(), duration: player.getDuration() });
      }
    }, 500);
  }
}

/** Singleton — chia sẻ 1 luồng phát cho toàn app. */
export const musicEngine = new MusicEngine();
