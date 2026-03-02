import React from 'react';
import './Badge.css';

const STATUS_META = {
  present: { label: 'Present', color: '#22c55e', bg: '#dcfce7', dot: '#16a34a' },
  absent: { label: 'Absent', color: '#ef4444', bg: '#fee2e2', dot: '#dc2626' },
  late: { label: 'Late', color: '#f59e0b', bg: '#fef3c7', dot: '#d97706' },
  excused: { label: 'Excused', color: '#6366f1', bg: '#ede9fe', dot: '#4f46e5' },
};

const Badge = ({ status }) => {
  const meta = STATUS_META[status] || STATUS_META.present;
  
  return (
    <span className="badge" style={{ background: meta.bg, color: meta.color }}>
      <span className="badge-dot" style={{ background: meta.dot }} />
      {meta.label}
    </span>
  );
};

export default Badge;