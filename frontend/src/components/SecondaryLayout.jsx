import s from './css/SecondaryLayout.module.css';
import Rail from './Rail';

export default function SecondaryLayout({ children }) {
  return (
    <div className={s.layout}>
      <Rail />
      <div className={s.content}>
        {children}
      </div>
    </div>
  );
}
