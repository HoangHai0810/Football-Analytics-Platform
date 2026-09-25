import React from 'react';

export default function RadarChart({
  stats1,
  name1 = 'Player 1',
  color1 = '#10b981',
  stats2 = null,
  name2 = 'Player 2',
  color2 = '#06b6d4',
  size = 320
}) {
  const dimensions = [
    { key: 'finishing_rating', label: 'Finishing' },
    { key: 'creation_rating', label: 'Creation' },
    { key: 'progression_rating', label: 'Progression' },
    { key: 'pressing_rating', label: 'Pressing' },
    { key: 'defending_rating', label: 'Defending' },
    { key: 'aerial_rating', label: 'Aerial' }
  ];

  const center = size / 2;
  const radius = size * 0.38;
  const numAxes = dimensions.length;
  const angleStep = (Math.PI * 2) / numAxes;

  // Concentric polygon levels
  const levels = [20, 40, 60, 80, 100];

  const getCoordinates = (value, index) => {
    const angle = index * angleStep - Math.PI / 2;
    const r = (value / 100) * radius;
    const x = center + r * Math.cos(angle);
    const y = center + r * Math.sin(angle);
    return { x, y };
  };

  const createPolygonPoints = (statsObj) => {
    if (!statsObj) return '';
    return dimensions
      .map((dim, i) => {
        const val = statsObj[dim.key] ?? 50;
        const { x, y } = getCoordinates(val, i);
        return `${x},${y}`;
      })
      .join(' ');
  };

  return (
    <div className="radar-wrapper" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        <defs>
          <radialGradient id="radar-bg-glow" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="rgba(16, 185, 129, 0.08)" />
            <stop offset="100%" stopColor="transparent" />
          </radialGradient>
        </defs>

        {/* Background circle */}
        <circle cx={center} cy={center} r={radius} fill="url(#radar-bg-glow)" />

        {/* Concentric rings */}
        {levels.map((lvl) => {
          const ringPoints = dimensions
            .map((_, i) => {
              const { x, y } = getCoordinates(lvl, i);
              return `${x},${y}`;
            })
            .join(' ');
          return (
            <polygon
              key={lvl}
              points={ringPoints}
              fill="none"
              stroke="rgba(255, 255, 255, 0.08)"
              strokeWidth="1"
            />
          );
        })}

        {/* Axis radial spokes */}
        {dimensions.map((dim, i) => {
          const { x, y } = getCoordinates(100, i);
          const labelAngle = i * angleStep - Math.PI / 2;
          const labelR = radius + 20;
          const lx = center + labelR * Math.cos(labelAngle);
          const ly = center + labelR * Math.sin(labelAngle);

          return (
            <g key={dim.key}>
              <line
                x1={center}
                y1={center}
                x2={x}
                y2={y}
                stroke="rgba(255, 255, 255, 0.12)"
                strokeWidth="1"
              />
              <text
                x={lx}
                y={ly}
                textAnchor="middle"
                dominantBaseline="central"
                fill="#94a3b8"
                fontSize="11.5"
                fontWeight="600"
                fontFamily="var(--font-heading)"
              >
                {dim.label}
              </text>
            </g>
          );
        })}

        {/* Player 1 Polygon */}
        {stats1 && (
          <g>
            <polygon
              points={createPolygonPoints(stats1)}
              fill={`${color1}33`}
              stroke={color1}
              strokeWidth="2.5"
              filter="drop-shadow(0 0 6px rgba(16, 185, 129, 0.5))"
            />
            {dimensions.map((dim, i) => {
              const val = stats1[dim.key] ?? 50;
              const { x, y } = getCoordinates(val, i);
              return (
                <circle
                  key={`p1-dot-${i}`}
                  cx={x}
                  cy={y}
                  r="3.5"
                  fill="#fff"
                  stroke={color1}
                  strokeWidth="2"
                />
              );
            })}
          </g>
        )}

        {/* Player 2 Polygon (for comparison) */}
        {stats2 && (
          <g>
            <polygon
              points={createPolygonPoints(stats2)}
              fill={`${color2}33`}
              stroke={color2}
              strokeWidth="2.5"
              filter="drop-shadow(0 0 6px rgba(6, 182, 212, 0.5))"
            />
            {dimensions.map((dim, i) => {
              const val = stats2[dim.key] ?? 50;
              const { x, y } = getCoordinates(val, i);
              return (
                <circle
                  key={`p2-dot-${i}`}
                  cx={x}
                  cy={y}
                  r="3.5"
                  fill="#fff"
                  stroke={color2}
                  strokeWidth="2"
                />
              );
            })}
          </g>
        )}
      </svg>

      {/* Legend if comparing */}
      {stats2 && (
        <div style={{ display: 'flex', gap: '20px', marginTop: '8px', fontSize: '13px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: color1 }} />
            <span style={{ color: '#fff', fontWeight: 600 }}>{name1}</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ width: '12px', height: '12px', borderRadius: '50%', backgroundColor: color2 }} />
            <span style={{ color: '#fff', fontWeight: 600 }}>{name2}</span>
          </div>
        </div>
      )}
    </div>
  );
}
