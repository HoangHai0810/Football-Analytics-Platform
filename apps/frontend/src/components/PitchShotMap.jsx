import React, { useState } from 'react';

export default function PitchShotMap({ shots = [], playerName = 'Player' }) {
  const [hoveredShot, setHoveredShot] = useState(null);
  const [outcomeFilter, setOutcomeFilter] = useState('ALL');

  const filteredShots = shots.filter((s) => {
    if (outcomeFilter === 'ALL') return true;
    return s.outcome === outcomeFilter;
  });

  const totalShots = shots.length;
  const goals = shots.filter((s) => s.outcome === 'GOAL').length;
  const totalXg = shots.reduce((acc, s) => acc + (s.xg || 0), 0);
  const xgPerShot = totalShots > 0 ? totalXg / totalShots : 0;
  const conversionRate = totalShots > 0 ? (goals / totalShots) * 100 : 0;

  const getOutcomeColor = (outcome) => {
    switch (outcome) {
      case 'GOAL':
        return '#10b981';
      case 'SAVED':
        return '#06b6d4';
      case 'BLOCKED':
        return '#f59e0b';
      case 'MISSED':
      default:
        return '#ef4444';
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      {/* Top summary stats bar */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(5, 1fr)',
          gap: '10px',
          background: 'rgba(0, 0, 0, 0.3)',
          borderRadius: 'var(--radius-md)',
          padding: '12px'
        }}
      >
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '18px', fontWeight: 800, color: '#fff' }}>{totalShots}</div>
          <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Total Shots</div>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '18px', fontWeight: 800, color: '#10b981' }}>{goals}</div>
          <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Goals Scored</div>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '18px', fontWeight: 800, color: '#38bdf8' }}>{totalXg.toFixed(2)}</div>
          <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Total xG</div>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '18px', fontWeight: 800, color: '#fbbf24' }}>{xgPerShot.toFixed(2)}</div>
          <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>xG / Shot</div>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '18px', fontWeight: 800, color: '#c084fc' }}>{conversionRate.toFixed(1)}%</div>
          <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Conversion</div>
        </div>
      </div>

      {/* Filter and Legend */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
        <div style={{ display: 'flex', gap: '6px' }}>
          {['ALL', 'GOAL', 'SAVED', 'BLOCKED', 'MISSED'].map((f) => (
            <button
              key={f}
              onClick={() => setOutcomeFilter(f)}
              style={{
                background: outcomeFilter === f ? 'rgba(16, 185, 129, 0.25)' : 'rgba(255, 255, 255, 0.05)',
                border: outcomeFilter === f ? '1px solid rgba(16, 185, 129, 0.5)' : '1px solid rgba(255, 255, 255, 0.08)',
                color: outcomeFilter === f ? '#34d399' : 'var(--text-secondary)',
                fontSize: '11px',
                padding: '4px 10px',
                borderRadius: 'var(--radius-sm)',
                cursor: 'pointer'
              }}
            >
              {f}
            </button>
          ))}
        </div>

        <div style={{ display: 'flex', gap: '14px', fontSize: '12px', alignItems: 'center' }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#10b981', boxShadow: '0 0 6px #10b981' }} />
            Goal
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#06b6d4' }} />
            Saved
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#f59e0b' }} />
            Blocked
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#ef4444' }} />
            Missed
          </span>
        </div>
      </div>

      {/* Pitch Canvas (Attacking Half: x: 60 to 120, y: 0 to 80) */}
      <div className="pitch-container">
        <svg viewBox="60 0 60 80" className="pitch-svg">
          <defs>
            {/* Pitch Grass Texture & Stripes */}
            <linearGradient id="pitchGrass" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stopColor="#064e3b" />
              <stop offset="50%" stopColor="#065f46" />
              <stop offset="100%" stopColor="#047857" />
            </linearGradient>

            <filter id="goalGlow" x="-50%" y="-50%" width="200%" height="200%">
              <feDropShadow dx="0" dy="0" stdDeviation="1.5" floodColor="#10b981" />
            </filter>
          </defs>

          {/* Grass Background */}
          <rect x="60" y="0" width="60" height="80" fill="url(#pitchGrass)" opacity="0.9" />

          {/* Alternating grass stripes */}
          {[0, 10, 20, 30, 40, 50].map((offset) => (
            <rect
              key={offset}
              x={60 + offset}
              y="0"
              width="5"
              height="80"
              fill="rgba(255, 255, 255, 0.02)"
            />
          ))}

          {/* Touchlines and Goal Line */}
          <line x1="60" y1="0" x2="120" y2="0" className="pitch-line" />
          <line x1="60" y1="80" x2="120" y2="80" className="pitch-line" />
          <line x1="120" y1="0" x2="120" y2="80" className="pitch-line" strokeWidth="1.2" />

          {/* Halfway line (at x=60) */}
          <line x1="60" y1="0" x2="60" y2="80" className="pitch-line" />

          {/* Center Circle Arc (at x=60, y=40, r=10) */}
          <path
            d="M 60,30 A 10,10 0 0,1 60,50"
            className="pitch-line"
          />
          <circle cx="60" cy="40" r="0.8" fill="rgba(255, 255, 255, 0.6)" />

          {/* Penalty Area: x from 102 to 120, y from 18 to 62 */}
          <rect
            x="102"
            y="18"
            width="18"
            height="44"
            className="pitch-line"
          />

          {/* Six Yard Box: x from 114 to 120, y from 30 to 50 */}
          <rect
            x="114"
            y="30"
            width="6"
            height="20"
            className="pitch-line"
          />

          {/* Penalty Spot: (108, 40) */}
          <circle cx="108" cy="40" r="0.7" fill="#fff" />

          {/* Penalty Arc outside box (center at 108, 40, r=10) */}
          <path
            d="M 102,32 A 10,10 0 0,0 102,48"
            className="pitch-line"
          />

          {/* Goal Mouth: x = 120, y from 36 to 44 */}
          <rect
            x="120"
            y="36"
            width="2.5"
            height="8"
            fill="none"
            stroke="rgba(255, 255, 255, 0.7)"
            strokeWidth="0.8"
          />

          {/* Corner Arcs */}
          <path d="M 120,2 A 2,2 0 0,0 118,0" className="pitch-line" />
          <path d="M 120,78 A 2,2 0 0,1 118,80" className="pitch-line" />

          {/* Trajectory lines from shot to center of goal (120, 40) on hover */}
          {hoveredShot && (
            <line
              x1={hoveredShot.x}
              y1={hoveredShot.y}
              x2="120"
              y2="40"
              stroke={getOutcomeColor(hoveredShot.outcome)}
              strokeDasharray="1.5,1.5"
              strokeWidth="0.8"
              opacity="0.75"
            />
          )}

          {/* Shots Plotting */}
          {filteredShots.map((shot) => {
            const color = getOutcomeColor(shot.outcome);
            const radius = Math.max(1.8, Math.min(5.5, (shot.xg || 0.1) * 6 + 1.2));
            const isGoal = shot.outcome === 'GOAL';

            return (
              <g
                key={shot.event_id}
                className="shot-marker"
                onMouseEnter={() => setHoveredShot(shot)}
                onMouseLeave={() => setHoveredShot(null)}
              >
                {/* Glow ring for goals */}
                {isGoal && (
                  <circle
                    cx={shot.x}
                    cy={shot.y}
                    r={radius + 1.2}
                    fill="none"
                    stroke="#10b981"
                    strokeWidth="0.6"
                    opacity="0.8"
                    filter="url(#goalGlow)"
                  />
                )}

                <circle
                  cx={shot.x}
                  cy={shot.y}
                  r={radius}
                  fill={color}
                  stroke="#fff"
                  strokeWidth="0.6"
                  opacity={hoveredShot && hoveredShot.event_id !== shot.event_id ? 0.4 : 0.95}
                />
              </g>
            );
          })}
        </svg>

        {/* Hover Tooltip Overlay */}
        {hoveredShot && (
          <div
            style={{
              position: 'absolute',
              bottom: '20px',
              right: '20px',
              background: 'rgba(15, 23, 42, 0.95)',
              border: `1px solid ${getOutcomeColor(hoveredShot.outcome)}`,
              borderRadius: 'var(--radius-md)',
              padding: '10px 14px',
              boxShadow: '0 8px 24px rgba(0, 0, 0, 0.7)',
              fontSize: '12px',
              minWidth: '180px'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
              <span style={{ fontWeight: 700, color: getOutcomeColor(hoveredShot.outcome) }}>
                {hoveredShot.outcome}
              </span>
              <span style={{ color: 'var(--text-muted)' }}>Min: {hoveredShot.minute}'</span>
            </div>
            <div style={{ color: '#fff', fontSize: '13px', fontWeight: 600 }}>
              xG: {hoveredShot.xg?.toFixed(2)}
            </div>
            <div style={{ color: 'var(--text-secondary)', marginTop: '4px' }}>
              Part: {hoveredShot.body_part || 'FOOT'} | {hoveredShot.situation || 'OPEN_PLAY'}
            </div>
            <div style={{ color: 'var(--text-muted)', fontSize: '11px', marginTop: '2px' }}>
              Coords: ({hoveredShot.x?.toFixed(1)}m, {hoveredShot.y?.toFixed(1)}m)
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
