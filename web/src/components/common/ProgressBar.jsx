import React from 'react';
import './ProgressBar.css';

const ProgressBar = ({ value, color = 'var(--accent)' }) => {
  return (
    <div className="progress-wrap">
      <div
        className="progress-bar"
        style={{ width: `${value}%`, background: color }}
      />
    </div>
  );
};

export default ProgressBar;