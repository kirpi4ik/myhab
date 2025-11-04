# Telegram Bot - Quick Start Guide

## Adding New Commands

### 1. Simple Action Command

```groovy
// In initializeCommands() method

def myCmd = new Command(
    id: "my_action",                    // Unique ID
    emoji: "🎯",                         // Icon
    label: "My Action",                  // Display name
    description: "Does something cool",  // Help text
    showInMenu: true,                    // Show in main menu
    type: CommandType.ACTION,            // Command type
    requiredRoles: ["ROLE_USER"],        // Who can use it
    handler: { User user ->              // What it does
        def message = createMessage("✅ Action executed!")
        message.setReplyMarkup(createMainMenu())
        return message
    }
)

commandRegistry["my_action"] = myCmd
```

### 2. Toggle Command (On/Off)

```groovy
def fanCmd = new Command(
    id: "fan",
    emoji: "🌀",
    label: "Fan Control",
    description: "Control ceiling fan",
    showInMenu: true,
    type: CommandType.TOGGLE,
    handler: { User user -> handleToggleCommand(user, "fan") }
)

commandRegistry["fan"] = fanCmd

// Add configuration:
// specialDevices.fan.peripheral.id: 5001
```

### 3. Menu with Sub-Commands

```groovy
// Parent menu
def climateCmd = new Command(
    id: "climate",
    emoji: "🌡️",
    label: "Climate Control",
    description: "Control temperature and humidity",
    showInMenu: true,
    type: CommandType.MENU,
    handler: { User user -> handleClimateMenuCommand(user) }
)

// Child commands
def acCmd = new Command(
    id: "climate_ac",
    emoji: "❄️",
    label: "Air Conditioning",
    type: CommandType.TOGGLE,
    handler: { User user -> handleToggleCommand(user, "climate_ac") }
)

def heaterCmd = new Command(
    id: "climate_heater",
    emoji: "🔥",
    label: "Heater",
    type: CommandType.TOGGLE,
    handler: { User user -> handleToggleCommand(user, "climate_heater") }
)

// Build hierarchy
climateCmd.addSubCommand(acCmd)
          .addSubCommand(heaterCmd)

// Register
commandRegistry["climate"] = climateCmd
commandRegistry["climate_ac"] = acCmd
commandRegistry["climate_heater"] = heaterCmd

// Implement handler
private SendMessage handleClimateMenuCommand(User user) {
    Command cmd = commandRegistry["climate"]
    def message = createMessage("🌡️ <b>Climate Control</b>\n\nSelect device:")
    message.setReplyMarkup(createSubMenu(cmd))
    return message
}
```

### 4. Confirmation Command

```groovy
def shutdownCmd = new Command(
    id: "shutdown",
    emoji: "⚡",
    label: "System Shutdown",
    description: "Shutdown the system",
    showInMenu: true,
    type: CommandType.CONFIRMATION,
    requiredRoles: ["ROLE_ADMIN"],  // Admin only!
    handler: { User user ->
        def message = createMessage("⚡ <b>System Shutdown</b>\n\n❗ Shutdown system? ❗")
        message.setReplyMarkup(createConfirmationKeyboard())
        return message
    }
)

commandRegistry["shutdown"] = shutdownCmd

// Handle confirmation in handleConfirmYesCommand:
case "shutdown":
    if (hasRequiredRole(user, previousCmd)) {
        publishShutdownEvent(user)
        message.setText("✅ System shutting down...")
        sendNotification(MessageLevel.WARNING, "System shutdown by <b>${user.userName}</b>")
    }
    break
```

## Command Types

| Type | Use Case | Example |
|------|----------|---------|
| `ACTION` | Direct action, no submenu | Send notification |
| `MENU` | Shows submenu | Lighting menu |
| `TOGGLE` | On/Off control | Light switch |
| `CONFIRMATION` | Requires yes/no | Open gate |

## Emoji Reference

### Common Icons

| Category | Emoji | Usage |
|----------|-------|-------|
| **Home** | 🏠 | Main menu, home |
| **Help** | ❓ | Help, info |
| **Success** | ✅ | Confirmation, success |
| **Error** | ⛔ | Cancel, error |
| **Light** | 💡 | Lighting |
| **Sun/Moon** | ☀️🌙 | On/Off |
| **Door** | 🚪 | Gate, door |
| **Water** | 💧 | Water control |
| **Fire** | 🔥 | Heating |
| **Snow** | ❄️ | Cooling, AC |
| **Fan** | 🌀 | Ventilation |
| **Temp** | 🌡️ | Temperature |
| **Power** | ⚡ | Power, energy |
| **Security** | 🔒 | Lock, security |
| **Camera** | 📹 | Camera, monitoring |
| **Music** | 🎵 | Audio |
| **TV** | 📺 | Entertainment |
| **Car** | 🚗 | Garage |
| **Garden** | 🌱 | Garden, plants |
| **Tools** | 🔧 | Technical |
| **Back** | ⬅️ | Navigation back |

## Configuration

### Device Mapping

```yaml
specialDevices:
  my_device:
    peripheral:
      id: 1234
  
  # Nested structure
  climate:
    ac:
      peripheral:
        id: 2001
    heater:
      peripheral:
        id: 2002
```

### Access in Code

```groovy
// Simple
def id = configProvider.get(Integer.class, "specialDevices.my_device.peripheral.id")

// Nested
def id = configProvider.get(Integer.class, "specialDevices.climate.ac.peripheral.id")

// Dynamic (from command ID)
private Integer getPeripheralId(String deviceId) {
    def configKey = "specialDevices.${deviceId.replaceAll('_', '.')}.peripheral.id"
    return configProvider.get(Integer.class, configKey)
}
```

## Event Publishing

### Light Event

```groovy
private void publishLightEvent(User user, Integer peripheralId, String action) {
    publish(TopicName.EVT_LIGHT.id(), new EventData().with {
        p0 = TopicName.EVT_LIGHT.id()
        p1 = EntityType.PERIPHERAL.name()
        p2 = peripheralId
        p3 = "Telegram bot: ${user.userName}"
        p4 = action  // "on" or "off"
        p5 = "{\"user\": \"${user.firstName} ${user.lastName}\"}"
        p6 = user.userName
        it
    })
}
```

### Custom Event

```groovy
private void publishMyEvent(User user, Integer peripheralId, String action) {
    publish(TopicName.EVT_MY_EVENT.id(), new EventData().with {
        p0 = TopicName.EVT_MY_EVENT.id()
        p1 = EntityType.PERIPHERAL.name()
        p2 = peripheralId
        p3 = "Telegram bot: ${user.userName}"
        p4 = action
        p5 = "{\"metadata\": \"value\"}"
        p6 = user.userName
        it
    })
}
```

## Notifications

### Send Notification

```groovy
// Info
sendNotification(MessageLevel.INFO, "Something happened")

// Success
sendNotification(MessageLevel.SUCCESS, "Action completed by <b>${user.userName}</b>")

// Warning
sendNotification(MessageLevel.WARNING, "Unauthorized attempt by <b>${user.userName}</b>")

// Error
sendNotification(MessageLevel.ERROR, "System error occurred")
```

## Role-Based Access

### Define Roles

```groovy
requiredRoles: ["ROLE_USER"]                    // Regular users
requiredRoles: ["ROLE_ADMIN"]                   // Admins only
requiredRoles: ["ROLE_USER", "ROLE_ADMIN"]      // Users and admins
requiredRoles: ["ROLE_SUPER_ADMIN"]             // Super admins only
```

### Check Roles

```groovy
// Automatic check in processCommand()
if (!hasRequiredRole(user, command)) {
    return createMessage("⛔ Insufficient permissions")
}

// Manual check
if (userService.tgUserHasAnyRole(user.userName, ["ROLE_ADMIN"])) {
    // Admin-only logic
}
```

## Testing

### Test New Command

1. **Start bot**: `/start`
2. **Find command**: Should appear in menu
3. **Execute**: Click command button
4. **Verify**: Check action executed
5. **Check logs**: Verify notification sent
6. **Test unauthorized**: Try with non-authorized user
7. **Test back button**: Navigate back works

### Debug Tips

```groovy
// Add logging
log.info("Command ${command.id} executed by ${user.userName}")

// Check command registry
log.info("Registered commands: ${commandRegistry.keySet()}")

// Verify configuration
log.info("Peripheral ID: ${getPeripheralId('my_device')}")
```

## Common Patterns

### Pattern 1: Simple Toggle

```groovy
def cmd = new Command(
    id: "device_name",
    emoji: "🎯",
    label: "Device Label",
    showInMenu: true,
    type: CommandType.TOGGLE,
    handler: { user -> handleToggleCommand(user, "device_name") }
)
```

### Pattern 2: Menu with Toggles

```groovy
def menuCmd = new Command(
    id: "category",
    emoji: "📁",
    label: "Category",
    showInMenu: true,
    type: CommandType.MENU,
    handler: { user -> handleCategoryMenuCommand(user) }
)

def item1Cmd = new Command(
    id: "category_item1",
    emoji: "🎯",
    label: "Item 1",
    type: CommandType.TOGGLE,
    handler: { user -> handleToggleCommand(user, "category_item1") }
)

menuCmd.addSubCommand(item1Cmd)
```

### Pattern 3: Confirmation Action

```groovy
def cmd = new Command(
    id: "critical_action",
    emoji: "⚠️",
    label: "Critical Action",
    type: CommandType.CONFIRMATION,
    requiredRoles: ["ROLE_ADMIN"],
    handler: { user ->
        def message = createMessage("⚠️ <b>Warning</b>\n\n❗ Proceed? ❗")
        message.setReplyMarkup(createConfirmationKeyboard())
        return message
    }
)

// In handleConfirmYesCommand:
case "critical_action":
    if (hasRequiredRole(user, previousCmd)) {
        // Execute action
        message.setText("✅ Action completed")
    }
    break
```

## Checklist for New Command

- [ ] Define Command object
- [ ] Set unique ID
- [ ] Add emoji and label
- [ ] Set command type
- [ ] Define required roles
- [ ] Implement handler
- [ ] Register in commandRegistry
- [ ] Add configuration (if needed)
- [ ] Implement event publishing (if needed)
- [ ] Add to handleConfirmYesCommand (if confirmation)
- [ ] Test with authorized user
- [ ] Test with unauthorized user
- [ ] Verify notifications sent
- [ ] Update documentation

## Quick Reference

### Command Definition Template

```groovy
def myCmd = new Command(
    id: "unique_id",
    emoji: "🎯",
    label: "Display Name",
    description: "Help text",
    showInMenu: true,
    type: CommandType.ACTION,
    requiredRoles: ["ROLE_USER"],
    handler: { User user ->
        // Implementation
        def message = createMessage("Result")
        message.setReplyMarkup(createMainMenu())
        return message
    }
)

commandRegistry["unique_id"] = myCmd
```

### Handler Template

```groovy
private SendMessage handleMyMenuCommand(User user) {
    Command cmd = commandRegistry["my_menu"]
    def message = createMessage("🎯 <b>Title</b>\n\nDescription:")
    message.setReplyMarkup(createSubMenu(cmd))
    return message
}
```

### Event Publishing Template

```groovy
private void publishMyEvent(User user, Integer peripheralId, String action) {
    publish(TopicName.EVT_MY_EVENT.id(), new EventData().with {
        p0 = TopicName.EVT_MY_EVENT.id()
        p1 = EntityType.PERIPHERAL.name()
        p2 = peripheralId
        p3 = "Telegram bot: ${user.userName}"
        p4 = action
        p5 = "{\"metadata\": \"value\"}"
        p6 = user.userName
        it
    })
}
```

---

**Happy Coding!** 🚀

