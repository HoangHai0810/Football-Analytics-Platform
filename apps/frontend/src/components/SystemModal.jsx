import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

export default function SystemModal({ onClose }) {
  const [statusInfo, setStatusInfo] = useState(null);

  useEffect(() => {
    async function loadStatus() {
      try {
        const res = await api.getSystemStatus();
        setStatusInfo(res.data);
      } catch (err) {
        console.error(err);
      }
    }
    loadStatus();
  }, []);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close-btn" onClick={onClose}>✕</button>

        <h2 style={{ fontSize: '20px', color: '#fff', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span>⚙️</span> Platform Status & Architecture Inspector
        </h2>
        <p style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '20px' }}>
          Real-time visibility into the end-to-end data engineering pipeline and platform services
        </p>

        {/* Pipeline Diagram */}
        <div style={{ background: 'rgba(0, 0, 0, 0.4)', padding: '16px', borderRadius: 'var(--radius-md)', marginBottom: '20px', fontSize: '12px', fontFamily: 'monospace', color: '#38bdf8' }}>
          <div style={{ color: 'var(--text-muted)', marginBottom: '6px' }}>DATA ARCHITECTURE FLOW:</div>
          <div>Data Source ➔ Kafka ➔ MinIO Lakehouse ➔ ClickHouse ➔ Spring Boot ➔ React</div>
        </div>

        {/* Status Entries */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '13px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '8px' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Backend Framework:</span>
            <span style={{ color: '#fff', fontWeight: 600 }}>Spring Boot 3.3.4 (REST API)</span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '8px' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Frontend Architecture:</span>
            <span style={{ color: '#fff', fontWeight: 600 }}>ReactJS 18 (Vite, Responsive Dark Theme)</span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '8px' }}>
            <span style={{ color: 'var(--text-secondary)' }}>ClickHouse Connection:</span>
            <span style={{ color: '#34d399', fontWeight: 600 }}>
              {statusInfo?.clickhouse_status || 'STANDBY (Local Container)'}
            </span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '8px' }}>
            <span style={{ color: 'var(--text-secondary)' }}>DE Lakehouse Status:</span>
            <span style={{ color: '#fbbf24', fontWeight: 600 }}>
              {statusInfo?.data_engineering_status || 'IN_PROGRESS'}
            </span>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '8px' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Active Data Engine:</span>
            <span style={{ color: '#c084fc', fontWeight: 600 }}>
              {statusInfo?.active_source || 'HIGH_FIDELITY_SEED_STORE'}
            </span>
          </div>
        </div>

        {/* Entity Counts */}
        {statusInfo?.entity_counts && (
          <div style={{ marginTop: '20px', background: 'rgba(255, 255, 255, 0.04)', padding: '14px', borderRadius: 'var(--radius-md)' }}>
            <div style={{ fontSize: '12px', fontWeight: 700, color: '#94a3b8', marginBottom: '8px', textTransform: 'uppercase' }}>
              Analytical Store Entities:
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px', textAlign: 'center' }}>
              <div>
                <div style={{ fontSize: '18px', fontWeight: 800, color: '#fff' }}>{statusInfo.entity_counts.competitions}</div>
                <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Competitions</div>
              </div>
              <div>
                <div style={{ fontSize: '18px', fontWeight: 800, color: '#fff' }}>{statusInfo.entity_counts.players}</div>
                <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Elite Players</div>
              </div>
              <div>
                <div style={{ fontSize: '18px', fontWeight: 800, color: '#10b981' }}>{statusInfo.entity_counts.shot_events}</div>
                <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Shot Events</div>
              </div>
            </div>
          </div>
        )}

        <div style={{ marginTop: '20px', textAlign: 'right' }}>
          <button
            onClick={onClose}
            className="filter-btn active"
            style={{ padding: '8px 20px' }}
          >
            Close Inspector
          </button>
        </div>
      </div>
    </div>
  );
}
