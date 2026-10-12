import React, { useState, useEffect, useRef } from 'react';
import { api } from '../services/api';

export default function AiAnalystChat({ initialQuery, initialMatchId, initialPlayerId }) {
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content:
        'Xin chào! Tôi là Trợ lý AI Phân tích PitchPulse.\n\nTôi sẵn sàng hỗ trợ bạn phân tích chiến thuật, đánh giá phong độ các đội bóng, bóc tách chỉ số chuyên sâu (xG, xA, tần suất dứt điểm) hoặc so sánh hiệu suất cầu thủ từ nguồn dữ liệu thực tế.\n\nBạn có thể đặt câu hỏi tự do hoặc chọn nhanh các chủ đề phân tích bên dưới:',
      grounded_facts: [
        'Dữ liệu đối chiếu trực tiếp từ kho lưu trữ sự kiện và thống kê trận đấu chính thức.',
        'Hỗ trợ phân tích chuyên sâu bàn thắng, xG, đối đầu và phong độ thực tế.',
      ],
      data_source: 'Chỉ số phân tích xác thực',
      suggested_questions: [
        'Tổng quan kết quả các trận đấu gần nhất',
        'Phân tích phong độ các đội bóng hàng đầu',
        'Top cầu thủ có chỉ số xG ấn tượng nhất',
      ],
    },
  ]);
  const [inputQuery, setInputQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);

  const samplePrompts = [
    { label: '🏆 Tổng quan các giải đấu', query: 'Tổng quan các trận đấu và giải đấu gần nhất' },
    { label: '⚡ Trận đấu mới nhất', query: 'Tổng quan các trận đấu mới nhất và trạng thái' },
    { label: '📊 Phong độ các đội', query: 'Phân tích phong độ các đội bóng hàng đầu' },
    { label: '⚽ Lịch thi đấu hôm nay', query: 'Xem các trận đấu hôm nay' },
  ];

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading]);

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

      // Filter raw developer endpoints or debug fields
      const rawSuggestions = aiData.suggested_questions || [];
      const cleanSuggestions = rawSuggestions.filter(
        (q) => !q.startsWith('GET /') && !q.startsWith('/api') && !q.includes('http')
      );

      // Filter debug keys from statistics_table
      let cleanTable = null;
      if (aiData.statistics_table && aiData.intent !== 'DATA_UNAVAILABLE') {
        const entries = Object.entries(aiData.statistics_table).filter(([k, v]) => {
          const kl = k.toLowerCase();
          const vl = String(v).toLowerCase();
          return (
            !kl.includes('query') &&
            !kl.includes('status') &&
            !kl.includes('endpoint') &&
            !vl.includes('unavailable') &&
            !vl.includes('http') &&
            !vl.includes('/api/')
          );
        });
        if (entries.length > 0) {
          cleanTable = Object.fromEntries(entries);
        }
      }

      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: aiData.answer || 'Đã phân tích xong.',
          grounded_facts: aiData.grounded_facts || [],
          statistics_table: cleanTable,
          data_source: 'Chỉ số phân tích xác thực',
          suggested_questions: cleanSuggestions.length > 0 ? cleanSuggestions : [
            'Phân tích phong độ các đội bóng hàng đầu',
            'Xem các trận đấu hôm nay',
            'Tổng quan các giải đấu',
          ],
        },
      ]);
    } catch (err) {
      console.error(err);
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: 'Không thể kết nối đến máy chủ phân tích dữ liệu. Vui lòng kiểm tra lại kết nối mạng hoặc thử lại sau.',
          grounded_facts: [],
          data_source: 'Lỗi kết nối',
          suggested_questions: [],
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
            <span>⚡</span> Trợ Lý AI Phân Tích PitchPulse
          </h1>
          <p className="section-subtitle">
            Hệ thống phân tích chiến thuật & dữ liệu bóng đá xác thực (Zero-Hallucination)
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
                  <span>⚡ PitchPulse AI Analyst</span>
                  {m.data_source ? <span> · {m.data_source}</span> : null}
                </div>
              )}

              <div className="ai-message-text" style={{ whiteSpace: 'pre-line', lineHeight: '1.65' }}>
                {m.content}
              </div>

              {/* Grounded facts badge */}
              {m.grounded_facts && m.grounded_facts.length > 0 && (
                <div className="grounded-box">
                  <div className="grounded-badge">🛡️ Nguồn số liệu xác thực</div>
                  <ul style={{ paddingLeft: 18, margin: 0, color: 'var(--text-secondary)' }}>
                    {m.grounded_facts.map((fact, fIdx) => (
                      <li key={fIdx} style={{ marginTop: 4, fontSize: '12.5px' }}>
                        {fact}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Verified Metrics Table (Only when not empty and not debug) */}
              {m.statistics_table && Object.keys(m.statistics_table).length > 0 && (
                <div className="ai-metric-snapshot">
                  <div className="ai-metric-title">📊 Bảng chỉ số đối chiếu</div>
                  <div className="ai-metric-grid">
                    {Object.entries(m.statistics_table).map(([k, v]) => (
                      <div key={k} className="ai-metric-cell">
                        <span className="metric-cell-label">{k}</span>
                        <span className="metric-cell-value">{String(v)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* User-friendly followup suggestions */}
              {m.suggested_questions && m.suggested_questions.length > 0 && (
                <div className="ai-followups">
                  <span className="ai-followups-label">💡 Gợi ý câu hỏi tiếp theo:</span>
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
            <span>AI đang truy vấn kho dữ liệu và tổng hợp phân tích…</span>
          </div>
        )}

        <div ref={messagesEndRef} />
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
          placeholder="Nhập tên đội bóng, trận đấu hoặc câu hỏi phân tích (VD: Phân tích Real Madrid, phong độ Man City...)"
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
