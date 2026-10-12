import React from 'react';

function TeamCrest({ src, name, size = 64 }) {
  const [err, setErr] = React.useState(false);
  const initials = (name || '?')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();

  if (!src || err) {
    return (
      <div
        className="team-crest-fallback"
        style={{
          width: size,
          height: size,
          fontSize: size * 0.35,
          borderRadius: 14,
          background: 'rgba(255, 255, 255, 0.08)',
          border: '1px solid rgba(255, 255, 255, 0.15)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontWeight: 800,
          color: '#f8fafc',
        }}
        title={name}
      >
        {initials || '?'}
      </div>
    );
  }
  return (
    <img
      src={src}
      alt={name}
      className="team-crest"
      style={{
        width: size,
        height: size,
        objectFit: 'contain',
        filter: 'drop-shadow(0 6px 14px rgba(0,0,0,0.6))',
      }}
      onError={() => setErr(true)}
    />
  );
}

export default function MatchDetailModal({ match, onClose, onAskAi }) {
  if (!match) return null;

  const d = match.match_date ? new Date(match.match_date) : null;
  const isDateOnly = d && d.getUTCHours() === 0 && d.getUTCMinutes() === 0 && d.getUTCSeconds() === 0;
  const kickoffTime = d && !isDateOnly
    ? d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
    : '';
  const kickoffDate = d
    ? d.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })
    : 'Chưa rõ thời gian';

  const isLive = match.status === 'LIVE';
  const isFinished = match.status === 'FINISHED';
  const showScore = isLive || isFinished;

  const hasXg = (match.home_xg != null && match.home_xg > 0) || (match.away_xg != null && match.away_xg > 0);
  const totalXg = (match.home_xg || 0) + (match.away_xg || 0);
  const homeXgPct = totalXg > 0 ? ((match.home_xg || 0) / totalXg) * 100 : 50;

  const handleAskAi = () => {
    if (onAskAi) {
      const prompt = `Phân tích trận đấu giữa ${match.home_team_name} và ${match.away_team_name} (${match.home_score} - ${match.away_score}) tại giải ${match.competition_name}`;
      onAskAi(prompt, match.match_id);
      onClose();
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-box match-detail-modal" onClick={(e) => e.stopPropagation()}>
        {/* Top Header */}
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontSize: '18px' }}>🏆</span>
            <div>
              <div style={{ fontWeight: 700, fontSize: '15px', color: '#f8fafc' }}>
                {match.competition_name || 'Giải đấu'}
              </div>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                {kickoffDate} {kickoffTime ? `· ${kickoffTime}` : ''}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span className={`status-badge ${(match.status || '').toLowerCase()}`}>
              {match.status === 'LIVE' ? '🔴 LIVE' : match.status === 'FINISHED' ? 'Kết thúc' : 'Sắp diễn ra'}
            </span>
            <button className="modal-close-btn" onClick={onClose} aria-label="Đóng">
              ✕
            </button>
          </div>
        </div>

        {/* Scoreboard Arena */}
        <div className="match-detail-scoreboard">
          <div className="match-team-col">
            <TeamCrest src={match.home_team_logo} name={match.home_team_name} size={72} />
            <h3 className="match-team-title">{match.home_team_name || 'Đội nhà'}</h3>
            <span className="match-team-role">Chủ nhà</span>
          </div>

          <div className="match-score-center">
            {showScore ? (
              <div className="match-score-num">
                <span>{match.home_score ?? 0}</span>
                <span className="match-score-sep">–</span>
                <span>{match.away_score ?? 0}</span>
              </div>
            ) : (
              <div className="match-score-vs">
                <span className="vs-badge">VS</span>
                {kickoffTime && <span className="kickoff-badge">{kickoffTime}</span>}
              </div>
            )}
            <div className="match-status-sub">{match.status}</div>
          </div>

          <div className="match-team-col">
            <TeamCrest src={match.away_team_logo} name={match.away_team_name} size={72} />
            <h3 className="match-team-title">{match.away_team_name || 'Đội khách'}</h3>
            <span className="match-team-role">Đội khách</span>
          </div>
        </div>

        {/* Expected Goals (xG) Bar */}
        {hasXg && (
          <div className="match-detail-card" style={{ marginTop: '16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '13px' }}>
              <span style={{ fontWeight: 700, color: '#34d399' }}>
                xG {(match.home_xg || 0).toFixed(2)}
              </span>
              <span style={{ color: 'var(--text-muted)', fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Kỳ vọng bàn thắng (Expected Goals)
              </span>
              <span style={{ fontWeight: 700, color: '#38bdf8' }}>
                xG {(match.away_xg || 0).toFixed(2)}
              </span>
            </div>
            <div className="xg-progress-track" style={{ height: '8px' }}>
              <div className="xg-fill-home" style={{ width: `${homeXgPct}%` }} />
              <div className="xg-fill-away" style={{ width: `${100 - homeXgPct}%` }} />
            </div>
            <div style={{ marginTop: '8px', fontSize: '12px', color: 'var(--text-secondary)', textAlign: 'center' }}>
              {match.home_xg > match.away_xg
                ? `${match.home_team_name} tạo ra nhiều cơ hội ăn bàn chất lượng hơn.`
                : match.away_xg > match.home_xg
                ? `${match.away_team_name} tạo ra nhiều cơ hội ăn bàn chất lượng hơn.`
                : 'Hai đội cân bằng về cơ hội nguy hiểm.'}
            </div>
          </div>
        )}

        {/* Match Information Grid */}
        <div className="match-info-grid">
          <div className="match-info-item">
            <div className="info-item-label">Sân vận động</div>
            <div className="info-item-val">{match.stadium || 'Chưa cập nhật'}</div>
          </div>
          <div className="match-info-item">
            <div className="info-item-label">Khán giả</div>
            <div className="info-item-val">
              {match.attendance ? `${match.attendance.toLocaleString('vi-VN')} người` : '—'}
            </div>
          </div>
          <div className="match-info-item">
            <div className="info-item-label">Thời gian thi đấu</div>
            <div className="info-item-val">{kickoffDate} {kickoffTime ? `(${kickoffTime})` : ''}</div>
          </div>
          <div className="match-info-item">
            <div className="info-item-label">Giải đấu / Mùa giải</div>
            <div className="info-item-val">{match.competition_name || 'League'}</div>
          </div>
        </div>

        {/* Modal Actions */}
        <div className="modal-actions">
          <button className="ask-ai-match-btn" onClick={handleAskAi}>
            <span>⚡</span>
            <span>Hỏi AI Phân Tích Trận Này</span>
          </button>
          <button className="filter-btn" onClick={onClose} style={{ padding: '10px 20px' }}>
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
}
