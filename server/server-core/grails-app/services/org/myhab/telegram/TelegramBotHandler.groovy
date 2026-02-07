package org.myhab.telegram


import grails.events.EventPublisher
import groovy.util.logging.Slf4j
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

/**
 * Telegram Bot Handler with extensible command structure
 * 
 * Features:
 * - Dynamic menu generation
 * - Hierarchical command structure
 * - Easy command addition
 * - Rich UI with emojis
 * - Role-based access control
 * - Command history tracking
 * 
 * Emoji reference:
 * https://emojipedia.org/
 * https://www.russellcottrell.com/greek/utilities/SurrogatePairCalculator.htm
 */
@Slf4j
@Component
class TelegramBotHandler extends TelegramLongPollingBot implements EventPublisher {
    ConfigProvider configProvider
    UserService userService
    TelegramService telegramService

    // User session context (command history per user)
    private static final Map<String, List<Command>> userContext = [:].asSynchronized()
    
    // Command registry for easy lookup
    private static final Map<String, Command> commandRegistry = [:]

    enum MessageLevel {
        INFO("ℹ️", "Info"),
        SUCCESS("✅", "Success"),
        WARNING("⚠️", "Warning"),
        ERROR("🛑", "Error")

        final String icon
        final String label

        MessageLevel(String icon, String label) {
            this.icon = icon
            this.label = label
        }
    }

    /**
     * Command definition with metadata and behavior
     */
    static class Command {
        String id
        String emoji
        String label
        String description
        List<String> requiredRoles = ["ROLE_USER"]
        boolean showInMenu = false
        CommandType type = CommandType.ACTION
        List<Command> subCommands = []
        Command parent = null
        Closure<SendMessage> handler = null
        String callbackPattern = null

        Command(Map config) {
            this.id = config.id
            this.emoji = config.emoji ?: "•"
            this.label = config.label
            this.description = config.description ?: label
            this.requiredRoles = config.requiredRoles ?: ["ROLE_USER"]
            this.showInMenu = config.showInMenu ?: false
            this.type = config.type ?: CommandType.ACTION
            this.handler = config.handler
            this.callbackPattern = config.callbackPattern ?: "^/${id}(@.+)?\$"
        }

        String getCommand() {
            return "/${id}"
        }

        String getDisplayName() {
            return "${emoji} ${label}"
        }

        String getFullDescription() {
            return "${emoji} ${label}\n${description}"
        }

        boolean matches(String text) {
            if (callbackPattern) {
                return text ==~ callbackPattern
            }
            return text == command || text == "/${id}@${text.split('@')[1]}"
        }

        Command addSubCommand(Command subCommand) {
            subCommand.parent = this
            subCommands.add(subCommand)
            return this
        }
    }

    enum CommandType {
        ACTION,      // Direct action (e.g., open gate)
        MENU,        // Shows submenu
        TOGGLE,      // On/Off action
        CONFIRMATION // Requires confirmation
    }

    @Override
    String getBotUsername() {
        return configProvider.get(String.class, "telegram.name")
    }

    @Override
    String getBotToken() {
        return configProvider.get(String.class, "telegram.token")
    }

    /**
     * Initialize command structure
     */
    void initializeCommands() {
        if (commandRegistry.isEmpty()) {
            // Main menu commands
            def helpCmd = new Command(
                id: "help",
                emoji: "❓",
                label: "Help",
                description: "Show available commands",
                showInMenu: true,
                type: CommandType.MENU,
                handler: { User user -> handleHelpCommand(user) }
            )

            def startCmd = new Command(
                id: "start",
                emoji: "🏠",
                label: "Start",
                description: "Show main menu",
                showInMenu: true,
                type: CommandType.MENU,
                handler: { User user -> handleStartCommand(user) }
            )

            // Gate control
            def gateCmd = new Command(
                id: "gate",
                emoji: "🚪",
                label: "Gate Control",
                description: "Open main gate",
                showInMenu: true,
                type: CommandType.CONFIRMATION,
                requiredRoles: ["ROLE_USER", "ROLE_ADMIN"],
                handler: { User user -> handleGateCommand(user) }
            )

            // Lighting control - hierarchical structure
            def lightCmd = new Command(
                id: "light",
                emoji: "💡",
                label: "Lighting",
                description: "Control lighting systems",
                showInMenu: true,
                type: CommandType.MENU,
                handler: { User user -> handleLightMenuCommand(user) }
            )

            // Exterior lighting
            def lightExtCmd = new Command(
                id: "light_ext",
                emoji: "🌙",
                label: "Exterior Lighting",
                description: "Control outdoor lights",
                type: CommandType.MENU,
                handler: { User user -> handleLightExtMenuCommand(user) }
            )

            def lightExtAllCmd = new Command(
                id: "light_ext_all",
                emoji: "🌟",
                label: "All Exterior",
                description: "Control all exterior lights",
                type: CommandType.TOGGLE,
                handler: { User user -> handleToggleCommand(user, "light_ext_all") }
            )

            def lightExtTerraceCmd = new Command(
                id: "light_ext_terrace",
                emoji: "🏡",
                label: "Terrace",
                description: "Control terrace lights",
                type: CommandType.TOGGLE,
                handler: { User user -> handleToggleCommand(user, "light_ext_terrace") }
            )

            def lightExtEntranceCmd = new Command(
                id: "light_ext_entrance",
                emoji: "🚪",
                label: "Entrance",
                description: "Control entrance lights",
                type: CommandType.TOGGLE,
                handler: { User user -> handleToggleCommand(user, "light_ext_entrance") }
            )

            // Interior lighting
            def lightIntCmd = new Command(
                id: "light_int",
                emoji: "🏠",
                label: "Interior Lighting",
                description: "Control indoor lights",
                type: CommandType.MENU,
                handler: { User user -> handleLightIntMenuCommand(user) }
            )

            def lightIntCtCmd = new Command(
                id: "light_int_ct",
                emoji: "🔧",
                label: "Technical Room",
                description: "Control technical room lights",
                type: CommandType.TOGGLE,
                handler: { User user -> handleToggleCommand(user, "light_int_ct") }
            )

            // Water control
            def waterCmd = new Command(
                id: "water",
                emoji: "💧",
                label: "Water Control",
                description: "Control exterior water",
                showInMenu: true,
                type: CommandType.TOGGLE,
                handler: { User user -> handleToggleCommand(user, "water") }
            )

            // Confirmation commands
            def yesCmd = new Command(
                id: "yes",
                emoji: "✅",
                label: "Yes",
                description: "Confirm action",
                callbackPattern: "^/yes(@.+)?\$",
                handler: { User user -> handleConfirmYesCommand(user) }
            )

            def noCmd = new Command(
                id: "no",
                emoji: "⛔",
                label: "No",
                description: "Cancel action",
                callbackPattern: "^/no(@.+)?\$",
                handler: { User user -> handleConfirmNoCommand(user) }
            )

            // Toggle action commands
            def onCmd = new Command(
                id: "on",
                emoji: "☀️",
                label: "Turn On",
                description: "Turn on",
                callbackPattern: "^/(\\w+)_on(@.+)?\$",
                handler: { User user -> handleOnOffCommand(user, "on") }
            )

            def offCmd = new Command(
                id: "off",
                emoji: "🌙",
                label: "Turn Off",
                description: "Turn off",
                callbackPattern: "^/(\\w+)_off(@.+)?\$",
                handler: { User user -> handleOnOffCommand(user, "off") }
            )

            // Build command hierarchy
            lightExtCmd.addSubCommand(lightExtAllCmd)
                      .addSubCommand(lightExtTerraceCmd)
                      .addSubCommand(lightExtEntranceCmd)

            lightIntCmd.addSubCommand(lightIntCtCmd)

            lightCmd.addSubCommand(lightExtCmd)
                   .addSubCommand(lightIntCmd)

            // Register all commands
            [helpCmd, startCmd, gateCmd, lightCmd, lightExtCmd, lightExtAllCmd, 
             lightExtTerraceCmd, lightExtEntranceCmd, lightIntCmd, lightIntCtCmd,
             waterCmd, yesCmd, noCmd, onCmd, offCmd].each { cmd ->
                commandRegistry[cmd.id] = cmd
            }

            log.info("Telegram bot commands initialized: ${commandRegistry.size()} commands registered")
        }
    }

    @Override
    void onUpdateReceived(Update update) {
        initializeCommands()

        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update)
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update)
        }
    }

    private void handleTextMessage(Update update) {
        String text = update.getMessage().getText()
        long chatId = update.getMessage().getChatId()
        User user = update.getMessage().getFrom()

        try {
            SendMessage message = processCommand(text, user, String.valueOf(chatId))
            if (message) {
                message.enableHtml(true)
                message.setParseMode(ParseMode.HTML)
                message.setChatId(String.valueOf(chatId))
                execute(message)
            }
        } catch (TelegramApiException e) {
            log.error("Telegram API exception in message handling", e)
            handleError(chatId, user, e)
        }
    }

    private void handleCallbackQuery(Update update) {
        String data = update.getCallbackQuery().getData()
        long chatId = update.getCallbackQuery().getMessage().getChatId()
        User user = update.getCallbackQuery().getFrom()

        try {
            SendMessage message = processCommand(data, user, String.valueOf(chatId))
            if (message) {
                message.enableHtml(true)
                message.setParseMode(ParseMode.HTML)
                message.setChatId(String.valueOf(chatId))
                execute(message)
            }
        } catch (TelegramApiException e) {
            log.error("Telegram API exception in callback query handling", e)
            handleError(chatId, user, e)
        }
    }

    private SendMessage processCommand(String text, User user, String chatId) {
        // Check authorization
        if (!telegramService.validTGUser(user.userName)) {
            sendNotification(MessageLevel.ERROR, "Unauthorized access by user: ${user.userName} in chat ${chatId}")
            return createMessage("⛔ Unauthorized access")
        }

        // Find matching command
        Command command = findCommand(text)
        
        if (!command) {
            log.warn("Unknown command: ${text} from user: ${user.userName}")
            return handleUnknownCommand(user)
        }

        // Check user roles
        if (!hasRequiredRole(user, command)) {
            sendNotification(MessageLevel.WARNING, "<b>${user.userName}</b> attempted to use ${command.label} without required roles [${command.requiredRoles.join(", ")}]")
            return createMessage("⛔ Insufficient permissions for this command")
        }

        // Log command usage
        sendNotification(MessageLevel.INFO, "<b>${user.userName}</b> invoked: ${command.displayName}")
        
        // Add to user context
        addToUserContext(user.userName, command)

        // Execute command handler
        if (command.handler) {
            return command.handler.call(user)
        }

        return handleUnknownCommand(user)
    }

    private Command findCommand(String text) {
        // Try exact match first
        for (Command cmd : commandRegistry.values()) {
            if (cmd.matches(text)) {
                return cmd
            }
        }

        // Try pattern matching for dynamic commands (on/off)
        if (text ==~ /^\/(\w+)_(on|off)(@.+)*/) {
            def matcher = text =~ /^\/(\w+)_(on|off)(@.+)*/
            if (matcher.matches()) {
                String action = matcher.group(2)
                return commandRegistry[action]
            }
        }

        return null
    }

    private boolean hasRequiredRole(User user, Command command) {
        return userService.tgUserHasAnyRole(user.userName, command.requiredRoles)
    }

    private void addToUserContext(String username, Command command) {
        if (!userContext[username]) {
            userContext[username] = []
        }
        userContext[username] << command
        
        // Keep only last 10 commands
        if (userContext[username].size() > 10) {
            userContext[username] = userContext[username].drop(userContext[username].size() - 10)
        }
    }

    private Command getLastCommand(String username) {
        def context = userContext[username]
        return context && context.size() > 0 ? context.last() : null
    }

    private Command getPreviousCommand(String username) {
        def context = userContext[username]
        return context && context.size() > 1 ? context[context.size() - 2] : null
    }

    private void clearUserContext(String username) {
        userContext[username] = []
    }

    // ==================== Command Handlers ====================

    private SendMessage handleStartCommand(User user) {
        def message = createMessage("🏠 <b>Welcome to MyHab Control</b>\n\nSelect an option:")
        message.setReplyMarkup(createMainMenu())
        return message
    }

    private SendMessage handleHelpCommand(User user) {
        StringBuilder helpText = new StringBuilder()
        helpText.append("❓ <b>Available Commands:</b>\n\n")
        
        commandRegistry.values()
            .findAll { it.showInMenu }
            .sort { it.label }
            .each { cmd ->
                helpText.append("${cmd.displayName}\n")
                helpText.append("<i>${cmd.description}</i>\n\n")
            }
        
        def message = createMessage(helpText.toString())
        message.setReplyMarkup(createMainMenu())
        return message
    }

    private SendMessage handleGateCommand(User user) {
        def message = createMessage("🚪 <b>Gate Control</b>\n\n❗ Open main gate? ❗")
        message.setReplyMarkup(createConfirmationKeyboard())
        return message
    }

    private SendMessage handleLightMenuCommand(User user) {
        Command lightCmd = commandRegistry["light"]
        def message = createMessage("💡 <b>Lighting Control</b>\n\nSelect area:")
        message.setReplyMarkup(createSubMenu(lightCmd))
        return message
    }

    private SendMessage handleLightExtMenuCommand(User user) {
        Command lightExtCmd = commandRegistry["light_ext"]
        def message = createMessage("🌙 <b>Exterior Lighting</b>\n\nSelect zone:")
        message.setReplyMarkup(createSubMenu(lightExtCmd))
        return message
    }

    private SendMessage handleLightIntMenuCommand(User user) {
        Command lightIntCmd = commandRegistry["light_int"]
        def message = createMessage("🏠 <b>Interior Lighting</b>\n\nSelect room:")
        message.setReplyMarkup(createSubMenu(lightIntCmd))
        return message
    }

    private SendMessage handleToggleCommand(User user, String deviceId) {
        Command lastCmd = getLastCommand(user.userName)
        def message = createMessage("${lastCmd.emoji} <b>${lastCmd.label}</b>\n\nSelect action:")
        message.setReplyMarkup(createToggleKeyboard(deviceId))
        return message
    }

    private SendMessage handleConfirmYesCommand(User user) {
        Command previousCmd = getPreviousCommand(user.userName)
        
        if (!previousCmd) {
            return handleUnknownCommand(user)
        }

        def message = createMessage()
        
        switch (previousCmd.id) {
            case "gate":
                if (hasRequiredRole(user, previousCmd)) {
                    publishGateOpenEvent(user)
                    message.setText("✅ Gate opened successfully! 🔓")
                    sendNotification(MessageLevel.SUCCESS, "Gate opened by <b>${user.userName}</b>")
                } else {
                    message.setText("⛔ Insufficient permissions")
                    sendNotification(MessageLevel.WARNING, "<b>${user.userName}</b> attempted to open gate without permissions")
                }
                break
            
            default:
                message.setText("⛔ Unknown action")
        }
        
        clearUserContext(user.userName)
        message.setReplyMarkup(createMainMenu())
        return message
    }

    private SendMessage handleConfirmNoCommand(User user) {
        Command previousCmd = getPreviousCommand(user.userName)
        
        def message = createMessage()
        if (previousCmd) {
            message.setText("⛔ Action cancelled: <i>${previousCmd.label}</i>")
        } else {
            message.setText("⛔ No action to cancel")
        }
        
        clearUserContext(user.userName)
        message.setReplyMarkup(createMainMenu())
        return message
    }

    private SendMessage handleOnOffCommand(User user, String action) {
        Command lastCmd = getLastCommand(user.userName)
        
        if (!lastCmd) {
            return handleUnknownCommand(user)
        }

        def deviceId = lastCmd.id
        def peripheralId = getPeripheralId(deviceId)
        
        if (!peripheralId) {
            return createMessage("⛔ Device configuration not found: ${deviceId}")
        }

        if (hasRequiredRole(user, lastCmd)) {
            publishLightEvent(user, peripheralId, action)
            
            def actionText = action == "on" ? "turned on ☀️" : "turned off 🌙"
            def message = createMessage("✅ ${lastCmd.label} ${actionText}")
            sendNotification(MessageLevel.SUCCESS, "<b>${user.userName}</b> ${actionText} ${lastCmd.label}")
            
            clearUserContext(user.userName)
            message.setReplyMarkup(createMainMenu())
            return message
        } else {
            sendNotification(MessageLevel.WARNING, "<b>${user.userName}</b> attempted to control ${lastCmd.label} without permissions")
            return createMessage("⛔ Insufficient permissions")
        }
    }

    private SendMessage handleUnknownCommand(User user) {
        def message = createMessage("⛔ Unknown command\n\nPlease select from menu:")
        message.setReplyMarkup(createMainMenu())
        return message
    }

    private void handleError(long chatId, User user, TelegramApiException e) {
        try {
            SendMessage errorMessage = createMessage("⛔ An error occurred. Please try again.")
            errorMessage.setChatId(String.valueOf(chatId))
            execute(errorMessage)
            
            sendInfoToSupport("Error from ${user.userName}: ${e.getMessage()}")
        } catch (TelegramApiException ex) {
            log.error("Failed to send error message", ex)
        }
    }

    // ==================== UI Creation Methods ====================

    private InlineKeyboardMarkup createMainMenu() {
        def keyboard = new InlineKeyboardMarkup()
        def buttons = []
        
        commandRegistry.values()
            .findAll { it.showInMenu }
            .sort { it.label }
            .each { cmd ->
                def button = new InlineKeyboardButton()
                button.setText(cmd.displayName)
                button.setCallbackData(cmd.command)
                buttons << [button]
            }
        
        keyboard.setKeyboard(buttons)
        return keyboard
    }

    private InlineKeyboardMarkup createSubMenu(Command parentCommand) {
        def keyboard = new InlineKeyboardMarkup()
        def buttons = []
        
        parentCommand.subCommands.each { cmd ->
            def button = new InlineKeyboardButton()
            button.setText(cmd.displayName)
            button.setCallbackData(cmd.command)
            buttons << [button]
        }
        
        // Add back button if has parent
        if (parentCommand.parent) {
            def backButton = new InlineKeyboardButton()
            backButton.setText("⬅️ Back")
            backButton.setCallbackData(parentCommand.parent.command)
            buttons << [backButton]
        } else {
            // Add main menu button
            def homeButton = new InlineKeyboardButton()
            homeButton.setText("🏠 Main Menu")
            homeButton.setCallbackData("/start")
            buttons << [homeButton]
        }
        
        keyboard.setKeyboard(buttons)
        return keyboard
    }

    private InlineKeyboardMarkup createConfirmationKeyboard() {
        def keyboard = new InlineKeyboardMarkup()
        
        def yesButton = new InlineKeyboardButton()
        yesButton.setText("✅ Yes")
        yesButton.setCallbackData("/yes")
        
        def noButton = new InlineKeyboardButton()
        noButton.setText("⛔ No")
        noButton.setCallbackData("/no")
        
        keyboard.setKeyboard([[yesButton, noButton]])
        return keyboard
    }

    private InlineKeyboardMarkup createToggleKeyboard(String deviceId) {
        def keyboard = new InlineKeyboardMarkup()
        
        def onButton = new InlineKeyboardButton()
        onButton.setText("☀️ Turn On")
        onButton.setCallbackData("/${deviceId}_on")
        
        def offButton = new InlineKeyboardButton()
        offButton.setText("🌙 Turn Off")
        offButton.setCallbackData("/${deviceId}_off")
        
        keyboard.setKeyboard([[onButton, offButton]])
        return keyboard
    }

    // ==================== Helper Methods ====================

    private SendMessage createMessage(String text = "") {
        def message = new SendMessage()
        if (text) {
            message.setText(text)
        }
        message.enableHtml(true)
        message.setParseMode(ParseMode.HTML)
        return message
    }

    private void sendNotification(MessageLevel level, String msg) {
        try {
            SendMessage message = new SendMessage()
            message.enableHtml(true)
            message.setText("${level.icon} ${msg}")
            message.setChatId(configProvider.get(String.class, "telegram.chanelId"))
            execute(message)
        } catch (TelegramApiException e) {
            log.error("Failed to send notification", e)
        }
    }

    private void sendInfoToSupport(String msg) throws TelegramApiException {
        SendMessage message = new SendMessage()
        message.setText(msg)
        message.setChatId(configProvider.get(String.class, "telegram.bot1x1ChannelId"))
        execute(message)
    }

    private Integer getPeripheralId(String deviceId) {
        def configKey = "specialDevices.${deviceId.replaceAll('_', '.')}.peripheral.id"
        try {
            return configProvider.get(Integer.class, configKey)
        } catch (Exception e) {
            log.warn("Failed to get peripheral ID for device: ${deviceId}, config key: ${configKey}")
            return null
        }
    }

    // ==================== Event Publishing ====================

    private void publishGateOpenEvent(User user) {
        publish(TopicName.EVT_INTERCOM_DOOR_LOCK.id(), new EventData().with {
            p0 = TopicName.EVT_INTERCOM_DOOR_LOCK.id()
            p1 = EntityType.PERIPHERAL.name()
            p2 = configProvider.get(Integer.class, "specialDevices.doorLockMain.peripheral.id")
            p3 = "Telegram bot: ${user.userName}"
            p4 = "open"
            p5 = "{\"unlockCode\": \"${user.firstName} ${user.lastName}\"}"
            p6 = user.userName
            it
        })
    }

    private void publishLightEvent(User user, Integer peripheralId, String action) {
        publish(TopicName.EVT_LIGHT.id(), new EventData().with {
            p0 = TopicName.EVT_LIGHT.id()
            p1 = EntityType.PERIPHERAL.name()
            p2 = peripheralId
            p3 = "Telegram bot: ${user.userName}"
            p4 = action
            p5 = "{\"user\": \"${user.firstName} ${user.lastName}\"}"
            p6 = user.userName
            it
        })
    }
}
