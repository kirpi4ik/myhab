package org.myhab.telegram


import grails.events.EventPublisher
import org.myhab.config.ConfigProvider
import org.myhab.domain.EntityType
import org.myhab.domain.events.TopicName
import org.myhab.domain.job.EventData
import org.myhab.services.TelegramService
import org.myhab.services.UserService
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

import static com.vdurmont.emoji.EmojiParser.parseToUnicode

/**
 * Emoji
 * https://emojipedia.org/
 * https://www.russellcottrell.com/greek/utilities/SurrogatePairCalculator.htm
 */
@Component
class TelegramBotHandler extends TelegramLongPollingBot implements EventPublisher {
    ConfigProvider configProvider
    UserService userService
    TelegramService telegramService

    enum MSG_LEVEL {
        INFO(":warning:"),
        WARNING(":warning:"),
        ERROR(":warning:")
        private final String icon

        MSG_LEVEL(def icon) {
            this.icon = icon
        }
    }
    def cmdContext = [];

    @Override
    public String getBotUsername() {
        return configProvider.get(String.class, "telegram.name");
    }

    @Override
    public String getBotToken() {
        return configProvider.get(String.class, "telegram.token");
    }

    private enum COMMANDS {
        YES("/yes", "DA"),
        NO("/no", "NU"),
        ON("/on", "Aprinde"),
        OFF("/off", "Stinge"),
        HELP("/help", "Ajutor", false),
        GATE("/gate", "Deschide poarta", true),
        LIGHT("/light", "Iluminat", true),
        LIGHT_EXT("/light_ext", "Iluminat exterior", false),
        LIGHT_EXT_ALL("/light_ext_all", "Iluminat Tot", false),
        LIGHT_EXT_TERRACE("/light_ext_terrace", "Iluminat terasa", false),
        LIGHT_EXT_ENTRANCE("/light_ext_entrance", "Iluminat terasa", false),
        LIGHT_INT("/light_int", "Iluminat interior", false),
        LIGHT_INT_CT("/light_int_ct", "Iluminat camera tehnica", false),
        WATER_EXT("/water", "Apa exterior", true);

        private final String command
        private final String label
        private final boolean selectable

        COMMANDS(def command, def label, selectable = false) {
            this.selectable = selectable
            this.label = label
            this.command = command;
        }

        static COMMANDS valueOfString(String cmdText) {
            return values().find { it.command.equalsIgnoreCase(cmdText) }
        }

        public String getCommand() {
            return command;
        }
    }

    private SendMessage getCommandResponse(String text, User user, String chatId) throws TelegramApiException {
        if (telegramService.validTGUser(user.userName)) {
            if (text.startsWith('/')) {
                def cmd = COMMANDS.valueOfString(text)
                sendMessage(MSG_LEVEL.INFO, "<b>${user.userName}</b> a invocat comanda ${text}")
                if (cmd) {
                    cmdContext << cmd
                    switch (cmd) {
                        case COMMANDS.HELP: {
                            return handleStartCommand(user)
                        } case COMMANDS.GATE: {
                            return handleGateCommand(user)
                        }
                        case [COMMANDS.LIGHT]: {
                            return handleLightLevel1Command(cmd, user)
                        }
                        case [COMMANDS.LIGHT_INT]: {
                            return handleLightLevel2IntCommand(cmd, user)
                        }
                        case [COMMANDS.LIGHT_EXT]: {
                            return handleLightLevel2ExtCommand(cmd, user)
                        }
                        case [COMMANDS.LIGHT_EXT_ALL, COMMANDS.LIGHT_EXT_TERRACE, COMMANDS.LIGHT_EXT_ENTRANCE, COMMANDS.LIGHT_INT_CT]: {
                            return handleLightOptionCmd(user)
                        };
                        case COMMANDS.YES: {
                            return handleConfirmYesCommand(user)
                        };
                        case COMMANDS.NO: {
                            return handleConfirmNOCommand(user)
                        };
                        case [COMMANDS.ON, COMMANDS.OFF]: {
                            return handleConfirmONOFFCommand(user)
                        }
                        default: return handleNotFoundCommand();
                    }
                } else {
                    return handleNotFoundCommand();
                }
            }
        } else {
            sendMessage(MSG_LEVEL.ERROR, "Unauthorized access by user: ${user.userName} in chat ${chatId}")
            SendMessage message = new SendMessage();
            message.setText("Unauthorized access");
            return message
        }
    }

    def sendMessage(MSG_LEVEL level, msg) {
        SendMessage message = new SendMessage();
        message.enableHtml(true)
        message.setText(parseToUnicode "${level.icon} ${msg}");
        message.setChatId(configProvider.get(String.class, "telegram.chanelId"));
        execute(message)
    }

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();

            try {
                SendMessage message = getCommandResponse(text, update.getMessage().getFrom(), String.valueOf(chat_id));
                if (message) {
                    message.enableHtml(true);
                    message.setParseMode(ParseMode.HTML);
                    message.setChatId(String.valueOf(chat_id));
                    execute(message);
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
                SendMessage message = handleNotFoundCommand();
                message.setChatId(String.valueOf(chat_id));
                try {
                    sendInfoToSupport("Error " + e.getMessage());
                    execute(message);
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            }
        } else if (update.hasCallbackQuery()) {
            try {
                SendMessage message = getCommandResponse(update.getCallbackQuery().getData(), update.getCallbackQuery().getFrom(), String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
                message.enableHtml(true);
                message.setParseMode(ParseMode.HTML);
                message.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
                try {
                    sendInfoToSupport("Error " + e.getMessage());
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    def sendInfoToSupport(String message) throws TelegramApiException {
        SendMessage messageSupport = new SendMessage();
        messageSupport.setText(message);
        messageSupport.setChatId(configProvider.get(String.class, "telegram.bot1x1ChannelId"));

        execute(messageSupport);
    }


    private SendMessage handleNotFoundCommand() {
        SendMessage message = new SendMessage();
        message.setText("⛔ Comanda invalida 😒");
        message.setReplyMarkup(mainMenuKeyboard());
        return message;
    }

    private SendMessage handleStartCommand(User user) {
        SendMessage message = new SendMessage();
        message.setText("ℹ️ Comenzi disponibile:");
        message.setReplyMarkup(mainMenuKeyboard());
        return message;
    }

    private SendMessage handleConfirmYesCommand(User user) {
        def message = new SendMessage()
        if (cmdContext.size() > 1) {
            switch (cmdContext[cmdContext.size() - 2]) {
                case COMMANDS.GATE: {
                    if (userService.tgUserHasAnyRole(user.userName, ["ROLE_USER", "ROLE_ADMIN"])) {
                        publish(TopicName.EVT_INTERCOM_DOOR_LOCK.id(), new EventData().with {
                            p0 = TopicName.EVT_INTERCOM_DOOR_LOCK.id()
                            p1 = EntityType.PERIPHERAL.name()
                            p2 = configProvider.get(Integer.class, "specialDevices.doorLockMain.peripheral.id")
                            p3 = "Telegram bot: ${user.userName}"
                            p4 = "open"
                            p5 = "{\"unlockCode\": \"$user.firstName $user.lastName\"}"
                            p6 = user.userName
                            it
                        })
                        message.setText("Poarta a fost deschisa 🔓 ")
                        sendMessage(MSG_LEVEL.INFO, "Poarta a fost deschisa de <b>${user.userName}</b>")
                    } else {
                        message.setText("⛔ Nu aveti suficient drepturi 😒")
                        sendMessage(MSG_LEVEL.WARNING, "<b>${user.userName}</b> a incercat sa deschida usa dar nu are drepturi")
                    }
                    cmdContext = []
                    break
                }
            }
        } else {
            message.setText("⛔ Comanda invalida 😒")
            message.setReplyMarkup(mainMenuKeyboard());
            sendMessage(MSG_LEVEL.WARNING, "<b>${user.userName}</b> | a introdus comanda gresita")
        }
        message
    }

    private SendMessage handleConfirmNOCommand(User user) {
        def message = new SendMessage()
        if (cmdContext.size() > 1) {
            message.setText("Comanda `<i>${(cmdContext[cmdContext.size() - 2] as COMMANDS).label}</i>` anulata de " + user.userName)
            cmdContext = []
        } else {
            message.setText("⛔ Comanda invalida 😒")
            message.setReplyMarkup(mainMenuKeyboard());
            cmdContext = []
            sendMessage(MSG_LEVEL.WARNING, "<b>${user.userName}</b> | a introdus comanda gresita")
        }
        return message
    }

    private SendMessage handleConfirmONOFFCommand(User user) {
        def actionCmd = (cmdContext[cmdContext.size() - 1] as COMMANDS)
        def message = new SendMessage()
        Integer id
        if (cmdContext.size() > 1) {
            def previousCmd = cmdContext[cmdContext.size() - 2] as COMMANDS
            switch (previousCmd) {
                case COMMANDS.LIGHT_EXT_ALL: {
                    id = configProvider.get(Integer.class, "specialDevices.light.ext.all.peripheral.id")
                    break
                }
                case COMMANDS.LIGHT_EXT_TERRACE: {
                    id = configProvider.get(Integer.class, "specialDevices.light.ext.terrace.peripheral.id")
                    break
                }
                case COMMANDS.LIGHT_EXT_ENTRANCE: {
                    id = configProvider.get(Integer.class, "specialDevices.light.ext.entrance.peripheral.id")
                    break
                }
                case COMMANDS.LIGHT_INT_CT: {
                    id = configProvider.get(Integer.class, "specialDevices.light.int.ct.peripheral.id")
                    break
                }
            }

            if (userService.tgUserHasAnyRole(user.userName, ["ROLE_USER", "ROLE_ADMIN"])) {
                publish(TopicName.EVT_LIGHT.id(), new EventData().with {
                    p0 = TopicName.EVT_LIGHT.id()
                    p1 = EntityType.PERIPHERAL.name()
                    p2 = id
                    p3 = "Telegram bot: ${user.userName}"
                    p4 = actionCmd == COMMANDS.ON ? "on" : actionCmd == COMMANDS.OFF ? "off" : "toggle"
                    p5 = "{\"user\": \"$user.firstName $user.lastName\"}"
                    p6 = user.userName
                    it
                })
                if (actionCmd == COMMANDS.ON) {
                    message.setText("Lumina a fost aprinsa pentru `${(cmdContext[cmdContext.size() - 2] as COMMANDS).label}`")
                } else {
                    message.setText("Lumina a fost stinsa pentru `${(cmdContext[cmdContext.size() - 2] as COMMANDS).label}` ")
                }
            } else {
                message.setText("⛔ Nu aveti suficient drepturi 😒")
                sendMessage(MSG_LEVEL.WARNING, "<b>${user.userName}</b> a incercat sa aprinda lumina dar nu are drepturi")
            }
            cmdContext = []

        } else {
            message.setText("⛔ Comanda invalida 😒")
            message.setReplyMarkup(mainMenuKeyboard());
        }
        message
    }

    private SendMessage handleGateCommand(User user) {
        SendMessage message = new SendMessage();
        message.setText("❗ Doriti sa deschideti poarta ❗❓");
        message.setReplyMarkup(getConfirmationKeyboard());
        return message;
    }

    private static InlineKeyboardMarkup getConfirmationKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton inlineKeyboardButtonYES = new InlineKeyboardButton();
        inlineKeyboardButtonYES.setText("✅ $COMMANDS.YES.label");
        inlineKeyboardButtonYES.setCallbackData(COMMANDS.YES.getCommand());

        InlineKeyboardButton inlineKeyboardButtonNO = new InlineKeyboardButton();
        inlineKeyboardButtonNO.setText("⛔ $COMMANDS.NO.label");
        inlineKeyboardButtonNO.setCallbackData(COMMANDS.NO.getCommand());

        inlineKeyboardMarkup.setKeyboard([[inlineKeyboardButtonYES, inlineKeyboardButtonNO]]);

        return inlineKeyboardMarkup;
    }

    private SendMessage handleLightOptionCmd(User user) {
        SendMessage message = new SendMessage();
        message.setText("💡 Iluminat `${(cmdContext[cmdContext.size() - 1] as COMMANDS).label}` 💡");
        message.setReplyMarkup(getOnOfKeyboard());
        return message;
    }

    private static InlineKeyboardMarkup getOnOfKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton inlineKeyboardButtonON = new InlineKeyboardButton();
        inlineKeyboardButtonON.setText(" ☀️ $COMMANDS.ON.label");
        inlineKeyboardButtonON.setCallbackData(COMMANDS.ON.getCommand());

        InlineKeyboardButton inlineKeyboardButtonOFF = new InlineKeyboardButton();
        inlineKeyboardButtonOFF.setText("🌙 $COMMANDS.OFF.label");
        inlineKeyboardButtonOFF.setCallbackData(COMMANDS.OFF.getCommand());

        inlineKeyboardMarkup.setKeyboard([[inlineKeyboardButtonON, inlineKeyboardButtonOFF]]);

        return inlineKeyboardMarkup;
    }

    private SendMessage handleLightLevel1Command(cmd, User user) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton lightExtBtn = new InlineKeyboardButton();
        lightExtBtn.setText("💡 $COMMANDS.LIGHT_EXT.label");
        lightExtBtn.setCallbackData(COMMANDS.LIGHT_EXT.command);

        InlineKeyboardButton lightIntBtn = new InlineKeyboardButton();
        lightIntBtn.setText("💡 $COMMANDS.LIGHT_INT.label");
        lightIntBtn.setCallbackData(COMMANDS.LIGHT_INT.command);

        inlineKeyboardMarkup.setKeyboard([[lightExtBtn], [lightIntBtn]]);
        SendMessage message = new SendMessage();
        message.setText("💡 $COMMANDS.LIGHT.label 💡");
        message.setReplyMarkup(inlineKeyboardMarkup);
        return message;
    }

    private SendMessage handleLightLevel2ExtCommand(cmd, User user) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton allBtn = new InlineKeyboardButton();
        allBtn.setText("💡 $COMMANDS.LIGHT_EXT_ALL.label");
        allBtn.setCallbackData(COMMANDS.LIGHT_EXT_ALL.command);

        InlineKeyboardButton terraceBtn = new InlineKeyboardButton();
        terraceBtn.setText("💡 $COMMANDS.LIGHT_EXT_TERRACE.label");
        terraceBtn.setCallbackData(COMMANDS.LIGHT_EXT_TERRACE.command);

        InlineKeyboardButton entranceBtn = new InlineKeyboardButton();
        entranceBtn.setText("💡 $COMMANDS.LIGHT_EXT_ENTRANCE.label");
        entranceBtn.setCallbackData(COMMANDS.LIGHT_EXT_ENTRANCE.command);

        inlineKeyboardMarkup.setKeyboard([[terraceBtn], [allBtn], [entranceBtn]]);
        SendMessage message = new SendMessage();
        message.setText("💡 $COMMANDS.LIGHT_EXT.label 💡");
        message.setReplyMarkup(inlineKeyboardMarkup);
        return message;
    }

    private SendMessage handleLightLevel2IntCommand(cmd, User user) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton ctBtn = new InlineKeyboardButton();
        ctBtn.setText("💡 $COMMANDS.LIGHT_INT_CT.label");
        ctBtn.setCallbackData(COMMANDS.LIGHT_INT_CT.command);

        inlineKeyboardMarkup.setKeyboard([[ctBtn]]);
        SendMessage message = new SendMessage();
        message.setText("💡 $COMMANDS.LIGHT_INT.label 💡");
        message.setReplyMarkup(inlineKeyboardMarkup);
        return message;
    }

    private InlineKeyboardMarkup mainMenuKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        def keyboardButtons = []
        COMMANDS.values().findAll { it.selectable }.each { cmd ->
            InlineKeyboardButton cmdButton = new InlineKeyboardButton();
            cmdButton.setText(cmd.label);
            cmdButton.setCallbackData(cmd.command);
            keyboardButtons << [cmdButton]
        }
        inlineKeyboardMarkup.setKeyboard(keyboardButtons);

        return inlineKeyboardMarkup;
    }
}
