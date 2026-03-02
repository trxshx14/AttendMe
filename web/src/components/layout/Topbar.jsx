import React from 'react';
import './Topbar.css';

const Topbar = ({ title, subtitle }) => {
  const today = new Date().toLocaleDateString('en-PH', {
    weekday: 'short',
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });

  return (
    <div className="topbar">
      <div className="topbar-left">
        <h2>{title}</h2>
        <p>{subtitle}</p>
      </div>
      <div className="topbar-right">
        <div className="date-badge">📅 {today}</div>
      </div>
    </div>
  );
};

export default Topbar;