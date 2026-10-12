export const METRICS_STORAGE_KEY = 'mm:metrics-settings';

export const METRICS_DEFAULTS = {
  refetchMs: 60_000,
  colors: {
    total:         '#4f8f24',
    vendor:        '#e6b01a',
    internal:      '#3a6b8a',
    avgVendor:     '#8a9280',
    firstResponse: '#8a9280',
  },
  thresholds: {
    avgVendor:     { warn: 1800, crit: 3600 },   // segundos (30m / 1h)
    firstResponse: { warn: 600,  crit: 1200 },   // segundos (10m / 20m)
  },
  targets: {
    avgVendor:     '',
    firstResponse: '',
  },
};

export const INTERVAL_OPTIONS = [
  { label: '1min',  value: 60_000    },
  { label: '5min',  value: 300_000   },
  { label: '10min', value: 600_000   },
  { label: '30min', value: 1_800_000 },
  { label: '1h',   value: 3_600_000 },
];

export const COLOR_FIELDS = [
  { key: 'total',         label: 'Tickets Ativos' },
  { key: 'vendor',        label: 'Com Vendor' },
  { key: 'internal',      label: 'Com Interno' },
  { key: 'avgVendor',     label: 'Méd. Vendor' },
  { key: 'firstResponse', label: 'First Response' },
];

export const THRESHOLD_FIELDS = [
  { key: 'avgVendor',     label: 'Méd. Vendor' },
  { key: 'firstResponse', label: 'First Response' },
];

export function loadMetricsSettings() {
  try {
    const raw = localStorage.getItem(METRICS_STORAGE_KEY);
    if (!raw) return METRICS_DEFAULTS;
    const parsed = JSON.parse(raw);
    return {
      ...METRICS_DEFAULTS,
      ...parsed,
      colors:     { ...METRICS_DEFAULTS.colors,     ...parsed.colors },
      thresholds: {
        avgVendor:     { ...METRICS_DEFAULTS.thresholds.avgVendor,     ...parsed.thresholds?.avgVendor },
        firstResponse: { ...METRICS_DEFAULTS.thresholds.firstResponse, ...parsed.thresholds?.firstResponse },
      },
      targets:    { ...METRICS_DEFAULTS.targets,    ...parsed.targets },
    };
  } catch {
    return METRICS_DEFAULTS;
  }
}

export function saveMetricsSettings(settings) {
  try { localStorage.setItem(METRICS_STORAGE_KEY, JSON.stringify(settings)); } catch {}
}
