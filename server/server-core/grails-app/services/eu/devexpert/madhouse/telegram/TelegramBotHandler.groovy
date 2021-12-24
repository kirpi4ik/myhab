package eu.devexpert.madhouse.telegram

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

/**
 * Emoji
 * https://apps.timwhitlock.info/emoji/tables/unicode
 * https://codepoints.net/U+1F680
 */
@Component
class TelegramBotHandler extends TelegramLongPollingBot {
    @Value('${telegram.name}')
    String name;

    @Value('${telegram.token}')
    String token;

    @Value('${telegram.chanelId}')
    String privateChannelId;

    @Value('${telegram.bot1x1ChannelId}')
    String bot1x1ChannelId;

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    private final String GATE_OPEN_LABEL = "Deschide poarta";
    private final String TEMP_WATER_LABEL = "Temperatura apa calda";
    private final String TEMP_INT_EXT_LABEL = "Informatii temperatura ambient";

    private enum COMMANDS {
        HELP("/help"),
        TEMP_INT_EXT("/temp all"),
        GATE("/gate"),
        TEMP_WATER("/temp water");

        private String command;

        COMMANDS(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }
    }

    def sendMessage(String level, msg) {
        SendMessage message = new SendMessage();
        message.setText("${level} | ${msg}");
        message.setChatId(String.valueOf(privateChannelId));
        execute(message)
    }

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chat_id = update.getMessage().getChatId();

            try {
                SendMessage message = getCommandResponse(text, update.getMessage().getFrom(), String.valueOf(chat_id));
                message.enableHtml(true);
                message.setParseMode(ParseMode.HTML);
                message.setChatId(String.valueOf(chat_id));
                execute(message);
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
        messageSupport.setChatId(bot1x1ChannelId);

        execute(messageSupport);
    }

    private SendMessage getCommandResponse(String text, User user, String chatId) throws TelegramApiException {
        switch (text) {
            case COMMANDS.GATE.getCommand(): return handleGateCommand();
            case COMMANDS.TEMP_WATER.getCommand(): return handleInfoTempCommand();
            case COMMANDS.HELP.getCommand(): return handleStartCommand();
            case COMMANDS.TEMP_INT_EXT.getCommand(): return handleStartCommand();
            default: return handleNotFoundCommand();
        }
    }

    private SendMessage handleNotFoundCommand() {
        SendMessage message = new SendMessage();
        message.setText("Comanda invalida");
        message.setReplyMarkup(getKeyboard());
        return message;
    }

    private SendMessage handleStartCommand() {
        SendMessage message = new SendMessage();
        message.setText("Comenzi disponibile:");
        message.setReplyMarkup(getKeyboard());
        return message;
    }

    private SendMessage handleGateCommand() {
        SendMessage message = new SendMessage();
        message.setText("Deschidere poarta !");
        message.setReplyMarkup(getKeyboard());
        return message;
    }

    private SendMessage handleInfoTempCommand() {
        SendMessage message = new SendMessage();
        message.setText("Tmperatura interior/exterior : xx C");
        message.setReplyMarkup(getKeyboard());
        return message;
    }

    private InlineKeyboardMarkup getKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();


        InlineKeyboardButton inlineKeyboardButtonAccess = new InlineKeyboardButton();
        inlineKeyboardButtonAccess.setText(GATE_OPEN_LABEL);
        inlineKeyboardButtonAccess.setCallbackData(COMMANDS.GATE.getCommand());

        InlineKeyboardButton inlineKeyboardButtonDemo = new InlineKeyboardButton();
        inlineKeyboardButtonDemo.setText(TEMP_INT_EXT_LABEL);
        inlineKeyboardButtonDemo.setCallbackData(COMMANDS.TEMP_INT_EXT.getCommand());

        InlineKeyboardButton inlineKeyboardButtonSuccess = new InlineKeyboardButton();
        inlineKeyboardButtonSuccess.setText(TEMP_WATER_LABEL);
        inlineKeyboardButtonSuccess.setCallbackData(COMMANDS.TEMP_WATER.getCommand());

        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(inlineKeyboardButtonAccess);

        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        keyboardButtonsRow2.add(inlineKeyboardButtonSuccess);

        List<InlineKeyboardButton> keyboardButtonsRow3 = new ArrayList<>();
        keyboardButtonsRow3.add(inlineKeyboardButtonDemo);

        keyboardButtons.add(keyboardButtonsRow1);
        keyboardButtons.add(keyboardButtonsRow3);
        keyboardButtons.add(keyboardButtonsRow2);

        inlineKeyboardMarkup.setKeyboard(keyboardButtons);

        return inlineKeyboardMarkup;
    }
}
