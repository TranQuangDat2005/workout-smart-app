import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Button from '../../components/Button';
import Icon from '../../components/Icon';
import Spinner from '../../components/Spinner';
import { feedApi, postMediaUrl, timeAgo } from '../../services/feedApi';
import type { CommentItem, FeedTab, PostItem } from '../../services/feedApi';
import { getUserIdFromToken } from '../../services/jwt';
import { tokenStorage } from '../../services/tokenStorage';

const AUDIENCE_OPTIONS = [
  { value: 'public', label: 'Công khai' },
  { value: 'friends', label: 'Bạn bè' },
  { value: 'private', label: 'Chỉ mình tôi' },
];

const MEDIA_ACCEPT = 'image/png,image/jpeg,image/webp';

function audienceLabel(value: string): string {
  return AUDIENCE_OPTIONS.find((o) => o.value === value)?.label ?? 'Công khai';
}

function avatarStyle(name: string): React.CSSProperties {
  return { background: `hsl(${(name.charCodeAt(0) * 13) % 360}, 50%, 30%)`, color: 'white' };
}

function apiErrorMessage(err: unknown, fallback: string): string {
  return (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback;
}

/* ── Ô soạn bài đăng ── */
function Composer({ onPosted }: { onPosted: (post: PostItem) => void }) {
  const [content, setContent] = useState('');
  const [audience, setAudience] = useState('public');
  const [media, setMedia] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [gifUrl, setGifUrl] = useState('');
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);
  const previewUrlRef = useRef<string | null>(null);

  useEffect(
    () => () => {
      if (previewUrlRef.current) URL.revokeObjectURL(previewUrlRef.current);
    },
    [],
  );

  const pickFile = (file: File | null) => {
    if (previewUrlRef.current) URL.revokeObjectURL(previewUrlRef.current);
    previewUrlRef.current = file ? URL.createObjectURL(file) : null;
    setMedia(file);
    setPreviewUrl(previewUrlRef.current);
  };

  const submit = async () => {
    if ((!content.trim() && !media && !gifUrl.trim()) || posting) return;
    setPosting(true);
    setError('');
    try {
      const post = await feedApi.createPost({
        content: content.trim() || undefined,
        audience,
        media,
        gifUrl: gifUrl.trim() || undefined,
      });
      setContent('');
      pickFile(null);
      setGifUrl('');
      setAudience('public');
      onPosted(post);
    } catch (err: unknown) {
      setError(apiErrorMessage(err, 'Đăng bài thất bại, vui lòng thử lại'));
    } finally {
      setPosting(false);
    }
  };

  return (
    <div className="card">
      <textarea
        className="input-field"
        style={{ minHeight: 84, resize: 'vertical', marginBottom: 12 }}
        placeholder="Chia sẻ buổi tập, thành tích hoặc động lực của bạn..."
        value={content}
        maxLength={2000}
        onChange={(e) => setContent(e.target.value)}
      />
      {media && previewUrl && (
        <div style={{ position: 'relative', marginBottom: 12 }}>
          <img className="feed-media" src={previewUrl} alt="Xem trước media" />
          <button
            className="btn-icon"
            style={{ position: 'absolute', top: 8, right: 8, background: 'rgba(0,0,0,0.6)' }}
            onClick={() => pickFile(null)}
            aria-label="Gỡ media"
          >
            <Icon name="trash" size={16} />
          </button>
        </div>
      )}
      {!media && gifUrl.trim() && (
        <div style={{ position: 'relative', marginBottom: 12 }}>
          <img
            className="feed-media"
            src={gifUrl.trim()}
            alt="Xem trước GIF"
            onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
          />
          <button
            className="btn-icon"
            style={{ position: 'absolute', top: 8, right: 8, background: 'rgba(0,0,0,0.6)' }}
            onClick={() => setGifUrl('')}
            aria-label="Gỡ GIF"
          >
            <Icon name="trash" size={16} />
          </button>
        </div>
      )}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
        <select
          className="input-field"
          style={{ width: 'auto' }}
          value={audience}
          onChange={(e) => setAudience(e.target.value)}
          aria-label="Đối tượng xem bài đăng"
        >
          {AUDIENCE_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
        <input
          ref={fileInputRef}
          type="file"
          hidden
          accept={MEDIA_ACCEPT}
          onChange={(e) => pickFile(e.target.files?.[0] ?? null)}
        />
        <Button variant="outlined" size="sm" onClick={() => fileInputRef.current?.click()}>
          <Icon name="image" size={15} style={{ marginRight: 6, verticalAlign: '-3px' }} />
          Ảnh
        </Button>
        <input
          className="input-field"
          style={{ width: 180, fontSize: 13 }}
          placeholder="URL GIF (tenor/instagram)"
          value={gifUrl}
          onChange={(e) => setGifUrl(e.target.value)}
          maxLength={500}
          aria-label="URL GIF"
        />
        <div style={{ marginLeft: 'auto' }}>
          <Button size="sm" loading={posting} disabled={!content.trim() && !media && !gifUrl.trim()} onClick={() => void submit()}>
            Đăng bài
          </Button>
        </div>
      </div>
      {error && <div className="notice notice-error" style={{ marginTop: 12 }}>{error}</div>}
    </div>
  );
}

/* ── Một bình luận ── */
function CommentRow({ comment }: { comment: CommentItem }) {
  const name = comment.displayName ?? 'Người dùng';
  return (
    <div style={{ display: 'flex', gap: 8 }}>
      <div className="avatar avatar-sm" style={{ ...avatarStyle(name), fontSize: 11 }}>
        {name.slice(0, 2).toUpperCase()}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 12.5, lineHeight: 1.5, wordBreak: 'break-word' }}>
          <span className="fw-600">{name}</span>{' '}
          <span className="text-secondary">{comment.content}</span>
        </div>
        <div className="text-muted" style={{ fontSize: 11 }}>{timeAgo(comment.createdAt)}</div>
      </div>
    </div>
  );
}

/* ── Một bài đăng ── */
function PostCard({ post, meId, onUpdate, onDelete }: {
  post: PostItem;
  meId: number | null;
  onUpdate: (post: PostItem) => void;
  onDelete: (postId: number) => void;
}) {
  const [commentText, setCommentText] = useState('');
  const [showComments, setShowComments] = useState(post.comments.length > 0);
  const [likeBusy, setLikeBusy] = useState(false);
  const [sending, setSending] = useState(false);

  const name = post.displayName ?? 'Người dùng';

  const like = async () => {
    if (likeBusy) return;
    setLikeBusy(true);
    try {
      const res = await feedApi.toggleLike(post.id);
      onUpdate({ ...post, likedByMe: res.liked, likeCount: res.likeCount });
    } catch {
      /* giữ nguyên trạng thái khi lỗi mạng */
    } finally {
      setLikeBusy(false);
    }
  };

  const addComment = async () => {
    const text = commentText.trim();
    if (!text || sending) return;
    setSending(true);
    try {
      const comment = await feedApi.addComment(post.id, text);
      setCommentText('');
      setShowComments(true);
      onUpdate({ ...post, comments: [...post.comments, comment] });
    } catch {
      /* giữ nguyên trạng thái khi lỗi mạng */
    } finally {
      setSending(false);
    }
  };

  const remove = async () => {
    if (!window.confirm('Xóa bài đăng này?')) return;
    try {
      await feedApi.deletePost(post.id);
      onDelete(post.id);
    } catch {
      /* giữ nguyên khi lỗi */
    }
  };

  return (
    <article className="card" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <div className="avatar avatar-md" style={avatarStyle(name)}>
          {name.slice(0, 2).toUpperCase()}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="fw-600" style={{ fontSize: 14 }}>{name}</div>
          <div className="text-muted" style={{ fontSize: 12 }}>{timeAgo(post.createdAt)}</div>
        </div>
        {post.audience !== 'public' && (
          <span className="badge badge-neutral" style={{ fontSize: 11 }}>{audienceLabel(post.audience)}</span>
        )}
        {meId != null && post.userId === meId && (
          <button className="btn-icon" onClick={() => void remove()} aria-label="Xóa bài đăng">
            <Icon name="trash" size={16} />
          </button>
        )}
      </div>

      {post.content && (
        <p
          className="text-secondary"
          style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', fontSize: 14, lineHeight: 1.6, margin: 0 }}
        >
          {post.content}
        </p>
      )}

      {post.mediaUrl && post.mediaType === 'image' && (
        <img className="feed-media" src={postMediaUrl(post.mediaUrl) ?? undefined} alt="Nội dung bài đăng" loading="lazy" />
      )}
      {post.gifUrl && (
        <img className="feed-media" src={post.gifUrl} alt="GIF bài đăng" loading="lazy" />
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
        <button
          className="feed-action"
          style={post.likedByMe ? { color: 'var(--text-negative)' } : undefined}
          onClick={() => void like()}
          aria-pressed={post.likedByMe}
          aria-label="Thích bài đăng"
        >
          <Icon
            name="heart"
            size={17}
            style={{ verticalAlign: '-3px', fill: post.likedByMe ? 'currentColor' : 'none' }}
          />
          <span>{post.likeCount > 0 ? post.likeCount : ''}</span>
        </button>
        <button
          className="feed-action"
          onClick={() => setShowComments((v) => !v)}
          aria-expanded={showComments}
          aria-label="Xem bình luận"
        >
          <Icon name="chat" size={17} style={{ verticalAlign: '-3px' }} />
          <span>{post.comments.length > 0 ? post.comments.length : ''}</span>
        </button>
      </div>

      {showComments && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, paddingTop: 4 }}>
          {post.comments.map((c) => (
            <CommentRow key={c.id} comment={c} />
          ))}
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              className="input-field"
              style={{ flex: 1 }}
              placeholder="Viết bình luận..."
              value={commentText}
              maxLength={1000}
              onChange={(e) => setCommentText(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') void addComment();
              }}
            />
            <Button
              size="sm"
              variant="dark"
              loading={sending}
              disabled={!commentText.trim()}
              onClick={() => void addComment()}
            >
              Gửi
            </Button>
          </div>
        </div>
      )}
    </article>
  );
}

/* ── Trang Cộng đồng: composer + 2 tab + infinite scroll ── */
export default function CommunityFeedPage() {
  const [tab, setTab] = useState<FeedTab>('friends');
  const [posts, setPosts] = useState<PostItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState('');

  const tabRef = useRef<FeedTab>('friends');
  const cursorRef = useRef<number | undefined>(undefined);
  const loadingRef = useRef(false);
  const doneRef = useRef(false);
  const sentinelRef = useRef<HTMLDivElement>(null);

  const meId = useMemo(() => {
    const token = tokenStorage.getAccessToken();
    return token ? getUserIdFromToken(token) : null;
  }, []);

  const loadMore = useCallback(async () => {
    if (loadingRef.current || doneRef.current) return;
    const requestedTab = tabRef.current;
    const cursor = cursorRef.current;
    loadingRef.current = true;
    setLoading(true);
    setError('');
    try {
      const page = await feedApi.feed(requestedTab, cursor);
      if (tabRef.current !== requestedTab) return; // response cũ của tab khác → bỏ
      setPosts((prev) => (cursor == null ? page.items : [...prev, ...page.items]));
      cursorRef.current = page.nextCursor ?? undefined;
      if (page.nextCursor == null) {
        doneRef.current = true;
        setDone(true);
      }
    } catch {
      if (tabRef.current === requestedTab) setError('Không thể tải bảng tin, vui lòng thử lại');
    } finally {
      loadingRef.current = false;
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadMore();
  }, [loadMore]);

  useEffect(() => {
    const el = sentinelRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) void loadMore();
      },
      { rootMargin: '300px' },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [loadMore]);

  const switchTab = (next: FeedTab) => {
    if (next === tab) return;
    setTab(next);
    tabRef.current = next;
    cursorRef.current = undefined;
    doneRef.current = false;
    setDone(false);
    setPosts([]);
    setError('');
    void loadMore();
  };

  const updatePost = (updated: PostItem) => {
    setPosts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
  };

  const removePost = (postId: number) => {
    setPosts((prev) => prev.filter((p) => p.id !== postId));
  };

  return (
    <div
      className="page-container"
      style={{ maxWidth: 640, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 20 }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
        <h1 style={{ margin: 0 }}>
          <Icon name="globe" size={22} style={{ verticalAlign: '-3px', marginRight: 8 }} /> Cộng đồng
        </h1>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className={`chip${tab === 'friends' ? ' selected' : ''}`} onClick={() => switchTab('friends')}>
            Bạn bè
          </button>
          <button className={`chip${tab === 'discover' ? ' selected' : ''}`} onClick={() => switchTab('discover')}>
            Khám phá
          </button>
        </div>
      </div>

      <Composer onPosted={(post) => setPosts((prev) => [post, ...prev])} />

      {error && <div className="notice notice-error">{error}</div>}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {posts.map((post) => (
          <PostCard key={post.id} post={post} meId={meId} onUpdate={updatePost} onDelete={removePost} />
        ))}
      </div>

      {loading && (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 12 }}>
          <Spinner />
        </div>
      )}
      {done && posts.length > 0 && (
        <p className="text-muted" style={{ textAlign: 'center', fontSize: 13 }}>Đã xem hết bài đăng</p>
      )}
      {!loading && posts.length === 0 && (
        <div className="card">
          <div className="empty-state" style={{ padding: '24px 0' }}>
            <div className="empty-state-icon"><Icon name="globe" size={42} /></div>
            <p className="empty-state-text">
              {tab === 'friends'
                ? 'Chưa có bài đăng nào từ bạn bè. Hãy đăng bài đầu tiên!'
                : 'Chưa có bài đăng công khai nào.'}
            </p>
          </div>
        </div>
      )}
      <div ref={sentinelRef} aria-hidden style={{ height: 1 }} />
    </div>
  );
}
