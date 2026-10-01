export const VOICE_ALIAS_KEY = 'feature.voice.alias';

/**
 * Voice aliases from an entity's configurations: every `feature.voice.alias` row,
 * comma-separated, trimmed and de-duplicated — the same reading as the server catalog.
 */
export function parseVoiceAliases(configurations) {
  const aliases = (configurations || [])
    .filter(cfg => cfg && cfg.key === VOICE_ALIAS_KEY)
    .flatMap(cfg => (cfg.value || '').split(','))
    .map(alias => alias.trim())
    .filter(Boolean);
  return [...new Set(aliases)];
}
