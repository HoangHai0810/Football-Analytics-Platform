import React, { useState } from 'react';
import { api } from '../services/api';

export default function AiAnalystChat() {
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content:
        'Xin chào — tôi là trợ lý phân tích PitchPulse. Mọi con số đều lấy từ kho dữ liệu đã ingest (không bịa số liệu). Hỏi về cầu thủ đã có trong hệ thống, hoặc chọn gợi ý bên dưới.',
      grounded_facts: [
        'Truy vấn thống kê mùa giải / trận từ PostgreSQL analytics.',
        'Nếu thiếu dữ liệu, hệ thống sẽ nói rõ — không ước lượng.',
      ],
      data_source: 'PostgreSQL analytics',
    },
  ]);
  const [inputQuery, setInputQuery] = useState('');
  const [loading, setLoading] = useState(false);

  const samplePrompts = [
    'Phân tích hiệu suất ghi bàn của một cầu thủ trong kho dữ liệu',
    'So sánh hai tiền đạo theo goals và xG',
    'Cầu thủ nào có nhiều kiến tạo nhất đã ingest?',
  ];

  const handleSend = async (queryText) => {
    const textToSend = queryText || inputQuery;
    if (!textToSend.trim()) return;

    const userMsg = { role: 'user', content: textToSend };
    setMessages((prev) => [...prev, userMsg]);
    setInputQuery('');
    setLoading(true);

    try {
      const res = await api.queryAiAnalyst(textToSend);
      const aiData = res.data;

      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: aiData.answer,
          grounded_facts: aiData.grounded_facts,
          statistics_table: aiData.statistics_table,
          data_source: aiData.data_source,
          suggested_questions: aiData.suggested_questions,
        },
      ]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: 'Không kết nối được dịch vụ phân tích. Thử lại sau.',
          grounded_facts: [],
          data_source: 'Error',
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ai-chat-card">
      <div className="section-header" style={{ marginBottom: 16 }}>
        <div>
          <h1 className="section-title">AI Phân tích</h1>
          <p className="section-subtitle">
            Trả lời dựa trên thống kê thật trong hệ thống
          </p>
        </div>
      </div>

      <div className="ai-suggestions-list">
        {samplePrompts.map((p, idx) => (
          <button key={idx} className="ai-suggestion-chip" onClick={() => handleSend(p)}>
            {p}
          </button>
        ))}
      </div>

      <div className="card ai-messages">
        {messages.map((m, index) => {
          const isUser = m.role === 'user';
          return (
            <div
              key={index}
              className={`ai-bubble ${isUser ? 'user' : 'assistant'}`}
            >
              {!isUser && (
                <div className="ai-bubble-label">
                  PitchPulse AI
                  {m.data_source ? ` · ${m.data_source}` : ''}
                </div>
              )}

              <div style={{ whiteSpace: 'pre-line' }}>{m.content}</div>

              {m.grounded_facts && m.grounded_facts.length > 0 && (
                <div className="grounded-box">
                  <div className="grounded-badge">Dữ liệu xác thực</div>
                  <ul style={{ paddingLeft: 18, color: 'var(--text-secondary)' }}>
                    {m.grounded_facts.map((fact, fIdx) => (
                      <li key={fIdx} style={{ marginTop: 3 }}>{fact}</li>
                    ))}
                  </ul>
                </div>
              )}

              {m.statistics_table && (
                <div className="ai-metric-snapshot">
                  <div className="ai-metric-title">Snapshot chỉ số</div>
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
                  <span className="ai-followups-label">Gợi ý tiếp theo</span>
                  <div className="ai-followups-chips">
                    {m.suggested_questions.map((q, qIdx) => (
                      <button key={qIdx} onClick={() => handleSend(q)} className="ai-followup-chip">
                        {q}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          );
        })}

        {loading && <div className="ai-loading">Đang phân tích…</div>}
      </div>

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
          placeholder="Hỏi về cầu thủ / chỉ số đã có trong hệ thống…"
          value={inputQuery}
          onChange={(e) => setInputQuery(e.target.value)}
        />
        <button type="submit" className="chat-send-btn" disabled={loading}>
          {loading ? '…' : 'Gửi'}
        </button>
      </form>
    </div>
  );
}
