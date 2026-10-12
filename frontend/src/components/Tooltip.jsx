import { useState } from 'react';
import s from './css/Tooltip.module.css';

export default function Tooltip({ children, content, position = 'top', delay = 200 }) {
  const [visible, setVisible] = useState(false);
  const [hovering, setHovering] = useState(false);

  const handleMouseEnter = () => {
    setHovering(true);
    const timer = setTimeout(() => setVisible(true), delay);
    return () => clearTimeout(timer);
  };

  const handleMouseLeave = () => {
    setHovering(false);
    setVisible(false);
  };

  return (
    <div
      className={s.tooltipContainer}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      {children}
      {hovering && (
        <div className={`${s.tooltip} ${s[`position-${position}`]} ${visible ? s.visible : ''}`}>
          {content}
          <div className={s.arrow} />
        </div>
      )}
    </div>
  );
}
