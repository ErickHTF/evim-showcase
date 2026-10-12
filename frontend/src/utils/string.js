export function statusSlug(name) {
  return name.toLowerCase().normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '').replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '');
}

export function getNameInitials(name) {
  if (!name) return '?';
  return name.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase();
}

export function textAdf(text) {
  return {
    version: 1, type: 'doc',
    content: [{ type: 'paragraph', content: [{ type: 'text', text }] }],
  };
}

export function formatBody(raw) {
  if (!raw) return '';
  let html = raw
    .replace(/\[img\]\(([^)]+)\)/g, '<img src="$1" alt="" />')
    .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
    .replace(/\n/g, '<br>');
  html = html.replace(
    /(?<!=["'])(?<![">])(https?:\/\/[^\s<"']+)/g,
    '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>'
  );
  return html;
}
