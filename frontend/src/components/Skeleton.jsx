export function Skeleton({ w = '100%', h = 12, r = 4, style, className = '' }) {
  return (
    <span
      className={`skeleton ${className}`}
      style={{ width: w, height: h, borderRadius: r, ...style }}
    />
  );
}

export function SkeletonAvatar({ size = 26 }) {
  return <Skeleton w={size} h={size} r="50%" />;
}

export function SkeletonRow({ lines = 1, gap = 6, lastWidth = '70%' }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap }}>
      {Array.from({ length: lines }).map((_, i) => (
        <Skeleton key={i} w={i === lines - 1 ? lastWidth : '100%'} h={11} />
      ))}
    </div>
  );
}

export function SkeletonTicketRow() {
  return (
    <div className="skeletonTicketRow" aria-hidden="true">
      <Skeleton w={14} h={14} r={3} />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6, minWidth: 0 }}>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <Skeleton w={70} h={10} />
          <Skeleton w={62} h={10} />
        </div>
        <Skeleton w="85%" h={13} />
        <div style={{ display: 'flex', gap: 8 }}>
          <Skeleton w={80} h={14} r={999} />
          <Skeleton w={32} h={10} />
        </div>
      </div>
      <SkeletonAvatar size={20} />
    </div>
  );
}

export default Skeleton;
