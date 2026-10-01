package org.myhab.services.voice

/**
 * Spoken confirmations for actions the fast path executed itself (the LLM is not
 * involved, so nothing else phrases the reply). The language follows the
 * client's locale, as the TTS voice does.
 */
class VoiceReplies {

    private static final Map<String, Map<String, String>> TEMPLATES = [
        en: [ON      : 'Turned on {0}.', OFF: 'Turned off {0}.', TOGGLE: 'Toggled {0}.',
             scenario: 'Started {0}.',
             START   : 'The mower is starting.', STOP: 'The mower is stopping.', PAUSE: 'The mower is paused.',
             RESUME  : 'The mower is resuming.', DOCK: 'The mower is heading back to its dock.'],
        ro: [ON      : 'Am pornit {0}.', OFF: 'Am oprit {0}.', TOGGLE: 'Am comutat {0}.',
             scenario: 'Am pornit scenariul {0}.',
             START   : 'Robotul de tuns pornește.', STOP: 'Robotul de tuns se oprește.',
             PAUSE   : 'Am pus robotul de tuns pe pauză.', RESUME: 'Robotul de tuns își reia lucrul.',
             DOCK    : 'Robotul de tuns se întoarce la bază.'],
        ru: [ON      : 'Включил {0}.', OFF: 'Выключил {0}.', TOGGLE: 'Переключил {0}.',
             scenario: 'Запустил сценарий {0}.',
             START   : 'Косилка запускается.', STOP: 'Косилка останавливается.', PAUSE: 'Косилка на паузе.',
             RESUME  : 'Косилка продолжает работу.', DOCK: 'Косилка возвращается на базу.'],
    ]

    static String confirm(FastDecision decision, String label, String locale) {
        Map<String, String> lang = TEMPLATES[language(locale)]
        String key = decision.intent == 'control' ? decision.action
                : decision.intent == 'scenario' ? 'scenario'
                : decision.mowerAction
        (lang[key] ?: TEMPLATES.en[key] ?: 'OK.').replace('{0}', label ?: '')
    }

    static String language(String locale) {
        String l = (locale ?: '').toLowerCase()
        l.startsWith('ro') ? 'ro' : l.startsWith('ru') ? 'ru' : 'en'
    }
}
