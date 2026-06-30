/**
 * Format a zoneless ISO timestamp (as delivered by Open-Meteo, e.g.
 * "2026-06-30T03:12") that represents a UTC instant into the given IANA
 * timezone. Values already carrying a 'Z' or numeric offset are passed through.
 *
 * Kept pure (timezone is a parameter) so it has no dependency on the auth
 * service — callers resolve the user's preferred zone and pass it in.
 *
 * @param {string} isoString zoneless-UTC (or offset-bearing) ISO timestamp
 * @param {string} [timeZone] IANA zone id; falsy = runtime default zone
 * @param {string} [locale] BCP-47 locale for formatting
 * @returns {string} "HH:MM" in the target zone, or "--:--" when unparseable
 */
export function formatTimeInZone(isoString, timeZone, locale = 'en-US') {
	if (!isoString) {
		return '--:--';
	}
	const hasZone = /[zZ]$|[+-]\d{2}:?\d{2}$/.test(isoString);
	const date = new Date(hasZone ? isoString : `${isoString}Z`);
	if (Number.isNaN(date.getTime())) {
		return '--:--';
	}
	return date.toLocaleTimeString(locale, {
		timeZone: timeZone || undefined,
		hour: '2-digit',
		minute: '2-digit',
	});
}
