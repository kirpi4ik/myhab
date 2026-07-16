package org.myhab.domain.ui

/**
 * Background image bytes for a {@link DashboardScreen} (floor plan, up to 5MB).
 *
 * <p>Kept in its own table with the FK on THIS side so that
 * {@code dashboardScreens} list/get queries never join or hydrate the bytes
 * (Hibernate cannot lazy-load a basic {@code byte[]} property without
 * bytecode instrumentation — the inline {@code User.avatar} pattern is only
 * viable for KB-sized payloads).</p>
 *
 * <p>Intentionally NOT exposed via GraphQL — bytes are streamed by
 * {@code DashboardScreenController} at {@code /api/screens/$id/background}.</p>
 */
class DashboardScreenBackground {

    static final int MAX_SIZE = 5 * 1024 * 1024

    byte[] data

    /** Owning side is DashboardScreen (hasOne): FK lives HERE, deletes cascade from the screen. */
    static belongsTo = [screen: DashboardScreen]

    static constraints = {
        screen unique: true
        data maxSize: MAX_SIZE
    }

    static mapping = {
        table '`dashboard_screen_backgrounds`'
        data type: 'binary'
    }
}
