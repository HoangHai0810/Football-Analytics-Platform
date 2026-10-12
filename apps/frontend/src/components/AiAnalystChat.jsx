import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

export default function AiAnalystChat({ initialQuery, initialMatchId, initialPlayerId }) {
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content:
        'Xin chào! Tôi là Trợ lý AI Phân tích PitchPulse. Dữ liệu của tôi được trích xuất trực tiếp từ kho phân tích PostgreSQL/ClickHouse, đảm bảo độ chính xác và không bịa đặt số liệu.\n\nBạn có thể hỏi về các trận đấu, phong độ đội bóng, chỉ số cầu thủ, hoặc bấm nhanh các câu hỏi gợi ý bên dưới.',
      grounded_facts: [
        'Dữ liệu lấy từ các trận đấu & sự kiện đã đồng bộ.',
        'Hỗ trợ phân tích chuyên sâu bàn thắng, xG, đối đầu và phong độ.',
      ],
      data_source: 'PitchPulse Data Lakehouse',
    },
  ]);
  const [inputQuery, setInputQuery] = useState('');
  const [loading, setLoading] = useState(false);

  const samplePrompts = [
    { label: '🏆 Tổng quan các giải đấu', query: 'Tổng quan các trận đấu và giải đấu gần nhất' },
    { label: '⚡ Trận đấu mới nhất', query: 'Tổng quan các trận đấu mới nhất và trạng thái' },
    { label: '📊 Phong độ các đội', query: 'Phân tích phong độ các đội bóng hàng đầu' },
    { label: '⚽ Lịch thi đấu hôm nay', query: 'Xem các trận đấu hôm nay' },
  ];

  const handleSend = async (queryText, matchId = null, playerId = null) => {
    const textToSend = queryText || inputQuery;
    if (!textToSend.trim()) return;

    const userMsg = { role: 'user', content: textToSend };
    setMessages((prev) => [...prev, userMsg]);
    setInputQuery('');
    setLoading(true);

    try {
      const payload = {
        query: textToSend,
        match_id: matchId || initialMatchId || null,
        player_id: playerId || initialPlayerId || null,
      };
      const res = await api.queryAiAnalyst(payload);
      const aiData = res.data;

      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: aiData.answer || 'Đã phân tích xong.',
          grounded_facts: aiData.grounded_facts || [],
          statistics_table: aiData.statistics_table || null,
          data_source: aiData.data_source || 'PostgreSQL Analytics',
          suggested_questions: aiData.suggested_questions || [],
        },
      ]);
    } catch (err) {
      console.error(err);
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: 'Không thể kết nối đến dịch vụ AI Analyst. Vui lòng kiểm tra lại kết nối mạng hoặc thử lại sau.',
          grounded_facts: [],
          data_source: 'Lỗi kết nối',
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (initialQuery) {
      handleSend(initialQuery, initialMatchId, initialPlayerId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialQuery, initialMatchId, initialPlayerId]);

  return (
    <div className="ai-chat-card">
      <div className="section-header" style={{ marginBottom: 16 }}>
        <div>
          <h1 className="section-title">
            <span>⚡</span> Trợ Lý AI Phân Tích
          </h1>
          <p className="section-subtitle">
            Phân tích chiến thuật & dữ liệu bóng đá xác thực (Zero-Hallucination)
          </p>
        </div>
      </div>

      {/* Verified Working Quick Prompt Chips */}
      <div className="ai-suggestions-list">
        {samplePrompts.map((p, idx) => (
          <button
            key={idx}
            className="ai-suggestion-chip"
            onClick={() => handleSend(p.query)}
            disabled={loading}
          >
            <span>{p.label}</span>
          </button>
        ))}
      </div>

      {/* Messages Feed */}
      <div className="card ai-messages">
        {messages.map((m, index) => {
          const isUser = m.role === 'user';
          return (
            <div key={index} className={`ai-bubble ${isUser ? 'user' : 'assistant'}`}>
              {!isUser && (
                <div className="ai-bubble-label">
                  <span>⚡ PitchPulse AI</span>
                  {m.data_source ? <span> · {m.data_source}</span> : null}
                </div>
              )}

              <div style={{ whiteSpace: 'pre-line', lineHeight: '1.6' }}>{m.content}</div>

              {m.grounded_facts && m.grounded_facts.length > 0 && (
                <div className="grounded-box">
                  <div className="grounded-badge">Dữ liệu nguồn xác thực</div>
                  <ul style={{ paddingLeft: 18, color: 'var(--text-secondary)' }}>
                    {m.grounded_facts.map((fact, fIdx) => (
                      <li key={fIdx} style={{ marginTop: 4 }}>
                        {fact}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {m.statistics_table && (
                <div className="ai-metric-snapshot">
                  <div className="ai-metric-title">Bảng thống kê chi tiết</div>
                  <div className="ai-metric-row">
                    {Object.entries(m.statistics_table).map(([k, v]) => (
                      <span key={k} style={{ color: 'var(--text-secondary)' }}>
                        <strong style={{ color: '#fff' }}>{k}:</strong> {String(v)}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {m.suggested_questions && m.suggested_questions.length > 0 && (
                <div className="ai-followups">
                  <span className="ai-followups-label">Câu hỏi tiếp theo có thể bạn quan tâm:</span>
                  <div className="ai-followups-chips">
                    {m.suggested_questions.map((q, qIdx) => (
                      <button
                        key={qIdx}
                        onClick={() => handleSend(q)}
                        className="ai-followup-chip"
                        disabled={loading}
                      >
                        {q}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          );
        })}

        {loading && (
          <div className="ai-loading">
            <span className="pulse-dot" />
            <span>AI đang truy vấn dữ liệu và phân tích trận đấu…</span>
          </div>
        )}
      </div>

      {/* Chat Input Bar */}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          handleSend();
        }}
        className="chat-input-bar"
      >
        <input
          type="text"
          className="chat-input"
          placeholder="Nhập tên đội bóng, trận đấu hoặc câu hỏi phân tích…"
          value={inputQuery}
          onChange={(e) => setInputQuery(e.target.value)}
          disabled={loading}
        />
        <button type="submit" className="chat-send-btn" disabled={loading || !inputQuery.trim()}>
          {loading ? '…' : 'Gửi'}
        </button>
      </form>
    </div>
  );
}
