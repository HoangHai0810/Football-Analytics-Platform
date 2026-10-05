import React, { useState } from 'react';
import { api } from '../services/api';

export default function AiAnalystChat() {
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content: "Xin chào! Tôi là **Trợ lý Phân tích Bóng đá AI (PitchPulse Analyst)**. Mọi phân tích và con số của tôi đều được trích xuất xác thực từ kho dữ liệu phân tích thống kê chuyên sâu, tuân thủ nguyên tắc **Zero-Hallucination** (Tuyệt đối không bịa đặt số liệu).\n\nBạn có thể hỏi tôi về chỉ số xG, so sánh cầu thủ, phong độ ghi bàn hoặc chọn gợi ý bên dưới:",
      grounded_facts: [
        "Truy vấn trực tiếp số liệu thống kê chi tiết theo mùa giải và trận đấu.",
        "Tất cả chỉ số per-90, tỷ lệ chuyển hóa cơ hội và xG/xA đều được tính toán chuẩn xác."
      ],
      data_source: "StatsBomb Verified Analytics"
    }
  ]);
  const [inputQuery, setInputQuery] = useState('');
  const [loading, setLoading] = useState(false);

  const samplePrompts = [
    "Phân tích hiệu suất dứt điểm của Erling Haaland so với xG",
    "So sánh đối đầu giữa Haaland và Kylian Mbappé",
    "Đánh giá năng lực sáng tạo và kiến tạo (xA) của Bukayo Saka",
    "Phân tích tầm ảnh hưởng của Kevin De Bruyne",
    "Đánh giá khả năng phòng ngự và phân phối bóng của Rodri"
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

      const assistantMsg = {
        role: 'assistant',
        content: aiData.answer,
        grounded_facts: aiData.grounded_facts,
        statistics_table: aiData.statistics_table,
        data_source: aiData.data_source,
        suggested_questions: aiData.suggested_questions
      };

      setMessages((prev) => [...prev, assistantMsg]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: "❌ Không thể kết nối với dịch vụ phân tích dữ liệu. Vui lòng thử lại sau.",
          grounded_facts: [],
          data_source: "Error"
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ai-chat-card">
      <div className="section-header" style={{ marginBottom: '16px' }}>
        <div>
          <h1 className="section-title">
            <span>🧠</span> AI Football Analyst
          </h1>
          <p className="section-subtitle">
            Trí tuệ nhân tạo phân tích chiến thuật và chỉ số cầu thủ dựa trên dữ liệu thống kê xác thực
          </p>
        </div>
      </div>

      {/* Suggested Query Chips */}
      <div className="ai-suggestions-list">
        {samplePrompts.map((p, idx) => (
          <button
            key={idx}
            className="ai-suggestion-chip"
            onClick={() => handleSend(p)}
          >
            💡 {p}
          </button>
        ))}
      </div>

      {/* Messages Feed */}
      <div
        className="card"
        style={{
          minHeight: '440px',
          maxHeight: '600px',
          overflowY: 'auto',
          display: 'flex',
          flexDirection: 'column',
          gap: '16px',
          padding: '24px'
        }}
      >
        {messages.map((m, index) => {
          const isUser = m.role === 'user';
          return (
            <div
              key={index}
              style={{
                alignSelf: isUser ? 'flex-end' : 'flex-start',
                maxWidth: isUser ? '80%' : '100%',
                background: isUser ? 'linear-gradient(135deg, rgba(16, 185, 129, 0.25), rgba(6, 182, 212, 0.25))' : 'rgba(15, 23, 42, 0.8)',
                border: isUser ? '1px solid rgba(16, 185, 129, 0.4)' : '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-lg)',
                padding: '16px 20px',
                color: '#fff',
                fontSize: '14px',
                lineHeight: '1.6'
              }}
            >
              {!isUser && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px', color: '#c084fc', fontWeight: 700, fontSize: '12px' }}>
                  <span>🤖</span> PITCHPULSE AI ANALYST &nbsp;|&nbsp; Nguồn dữ liệu: {m.data_source || 'StatsBomb Verified Analytics'}
                </div>
              )}

              <div style={{ whiteSpace: 'pre-line' }}>{m.content}</div>

              {/* Grounded facts container */}
              {m.grounded_facts && m.grounded_facts.length > 0 && (
                <div className="grounded-box">
                  <div className="grounded-badge">
                    <span>🛡️</span> Grounded Facts (Dữ liệu xác thực):
                  </div>
                  <ul style={{ paddingLeft: '18px', color: 'var(--text-secondary)' }}>
                    {m.grounded_facts.map((fact, fIdx) => (
                      <li key={fIdx} style={{ marginTop: '3px' }}>{fact}</li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Statistical table */}
              {m.statistics_table && (
                <div style={{ marginTop: '12px', background: 'rgba(0,0,0,0.3)', padding: '10px 14px', borderRadius: 'var(--radius-sm)', fontSize: '12.5px' }}>
                  <div style={{ fontWeight: 600, color: '#38bdf8', marginBottom: '6px' }}>📊 Metric Snapshot:</div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px' }}>
                    {Object.entries(m.statistics_table).map(([k, v]) => (
                      <span key={k} style={{ color: 'var(--text-secondary)' }}>
                        <strong style={{ color: '#fff' }}>{k}:</strong> {String(v)}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Follow-up suggestions */}
              {m.suggested_questions && m.suggested_questions.length > 0 && (
                <div style={{ marginTop: '14px', borderTop: '1px dashed var(--border-subtle)', paddingTop: '10px' }}>
                  <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Gợi ý câu hỏi tiếp theo:</span>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginTop: '6px' }}>
                    {m.suggested_questions.map((q, qIdx) => (
                      <button
                        key={qIdx}
                        onClick={() => handleSend(q)}
                        style={{
                          background: 'rgba(255, 255, 255, 0.05)',
                          border: '1px solid var(--border-subtle)',
                          color: '#c4b5fd',
                          fontSize: '11.5px',
                          padding: '4px 10px',
                          borderRadius: 'var(--radius-full)',
                          cursor: 'pointer'
                        }}
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
          <div style={{ color: '#c4b5fd', fontStyle: 'italic', fontSize: '13px' }}>
            ⚡ AI đang tổng hợp và phân tích dữ liệu trận đấu...
          </div>
        )}
      </div>

      {/* Input Bar */}
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
          placeholder="Nhập câu hỏi phân tích bóng đá (ví dụ: 'Phân tích hiệu suất dứt điểm của Haaland')..."
          value={inputQuery}
          onChange={(e) => setInputQuery(e.target.value)}
        />
        <button type="submit" className="chat-send-btn" disabled={loading}>
          {loading ? 'Analyzing...' : 'Gửi câu hỏi'}
        </button>
      </form>
    </div>
  );
}
