import React, { useState } from 'react';
import Header from './components/Header';
import MatchCenter from './components/MatchCenter';
import PlayerIntelligence from './components/PlayerIntelligence';
import PlayerComparison from './components/PlayerComparison';
import AiAnalystChat from './components/AiAnalystChat';

export default function App() {
  const [activeTab, setActiveTab] = useState('matches');
  const [comparedPlayerId, setComparedPlayerId] = useState(null);
  const [aiConfig, setAiConfig] = useState({ query: '', matchId: null, playerId: null });

  const handleComparePlayer = (playerId) => {
    setComparedPlayerId(playerId);
    setActiveTab('compare');
  };

  const handleAskAi = (query, matchId = null, playerId = null) => {
    setAiConfig({ query, matchId, playerId });
    setActiveTab('ai-analyst');
  };

  return (
    <div className="app-container">
      <Header activeTab={activeTab} setActiveTab={setActiveTab} />

      <main className="main-content">
        {activeTab === 'matches' && (
          <MatchCenter onAskAi={handleAskAi} />
        )}

        {activeTab === 'players' && (
          <PlayerIntelligence
            onComparePlayer={handleComparePlayer}
            onAskAi={handleAskAi}
          />
        )}

        {activeTab === 'compare' && (
          <PlayerComparison
            initialPlayerId={comparedPlayerId}
            onAskAi={handleAskAi}
          />
        )}

        {activeTab === 'ai-analyst' && (
          <AiAnalystChat
            initialQuery={aiConfig.query}
            initialMatchId={aiConfig.matchId}
            initialPlayerId={aiConfig.playerId}
          />
        )}
      </main>

      <footer className="app-footer">
        <div className="app-footer-inner">
          PitchPulse — Nền tảng phân tích bóng đá chuyên nghiệp từ nguồn dữ liệu thực tế
        </div>
      </footer>
    </div>
  );
}
