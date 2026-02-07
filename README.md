# myHAB - My Home Automation Backend

A comprehensive home automation platform for monitoring and controlling your smart home ecosystem. myHAB provides a unified interface for managing heating systems, solar energy production, weather data, lighting, security, and more.

**Website:** [http://myhab.org/](http://myhab.org/)

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [User Guide](#user-guide)
  - [Dashboard and Navigation](#dashboard-and-navigation)
  - [Device Control](#device-control)
  - [Energy Monitoring](#energy-monitoring)
  - [Heating Management](#heating-management)
  - [Weather Information](#weather-information)
  - [Telegram Bot Control](#telegram-bot-control)
  - [Mobile Access](#mobile-access)
- [Architecture](#architecture)
  - [System Overview](#system-overview)
  - [Technology Stack](#technology-stack)
  - [Communication Layer](#communication-layer)
  - [Data Flow](#data-flow)
  - [Domain Model](#domain-model)
  - [Event System](#event-system)
  - [Scheduled Tasks](#scheduled-tasks)
  - [Security](#security)
- [Developer Information](#developer-information)

---

## Overview

myHAB (My Home Automation Backend) is a full-stack smart home management system designed to integrate various IoT devices, sensors, and external services into a single, cohesive platform. The system supports real-time monitoring, automated control scenarios, and comprehensive data analytics for home energy management.

The platform excels at:
- **Unified Control** - Manage all your smart home devices from a single interface
- **Energy Intelligence** - Track solar production, grid consumption, and optimize energy usage
- **Climate Automation** - Automated heating control with heat pump integration
- **Real-time Monitoring** - Live updates via WebSocket and MQTT protocols
- **Remote Access** - Control your home from anywhere via web, mobile app, or Telegram

---

## Key Features

| Feature | Description |
|---------|-------------|
| **Multi-Device Support** | Control ESP32 controllers, MEGAD devices, ONVIF cameras, and more |
| **Solar Monitoring** | Real-time tracking of Huawei solar inverter production and grid export |
| **Heat Pump Integration** | Full NIBE F1145 heat pump monitoring with temperature sensors and energy stats |
| **Weather Station** | Automatic weather data from Open-Meteo with forecasts |
| **Telegram Bot** | Remote control via Telegram with role-based access |
| **PWA Support** | Install as a native app on mobile devices |
| **Scenario Automation** | Create custom automation scenarios with triggers and actions |
| **Time-Series Data** | Historical data storage and statistics for energy analysis |
| **Multi-Zone Support** | Organize devices by zones and layers within your home |

---

## User Guide

### Dashboard and Navigation

The myHAB dashboard provides a comprehensive overview of your home automation system:

- **Zone Navigation** - Browse devices organized by location (living room, bedroom, garage, etc.)
- **Device Categories** - View devices grouped by type (lighting, heating, sensors, etc.)
- **Quick Actions** - One-click control for frequently used devices and scenarios
- **Status Indicators** - Real-time status badges showing device connectivity and states
- **Favorites** - Mark commonly used jobs and scenarios as favorites for quick access

The navigation system supports breadcrumb trails, allowing you to easily navigate through the hierarchy of zones, devices, and peripherals.

### Device Control

Control your connected devices with an intuitive interface:

- **Lighting** - Turn lights on/off, adjust brightness, and control RGB colors
- **Power Outlets** - Switch smart plugs and monitor power consumption
- **Gates and Doors** - Open/close gates with confirmation dialogs for safety
- **Intercom** - Integration with door entry systems
- **Scenarios** - Execute predefined automation scenarios with a single tap

Each device shows its current state, last update time, and provides appropriate controls based on device type. The system supports:
- Toggle switches for binary devices
- Sliders for dimmable lights
- Confirmation dialogs for critical actions (gates, locks)
- Real-time state synchronization across all connected clients

### Energy Monitoring

Track your home's energy production and consumption:

**Solar Production (Huawei Inverter)**
- Current power output in real-time
- Daily, monthly, and lifetime energy production
- Grid export and self-consumption metrics
- Inverter efficiency and temperature
- Per-phase grid connection data

**Grid Consumption**
- Phase-by-phase power consumption (A, B, C)
- Grid import/export balance
- Voltage, current, and frequency monitoring
- Historical consumption trends

**Energy Statistics**
- Hourly, daily, and monthly aggregated statistics
- Trend analysis and comparisons
- Export data for further analysis

### Heating Management

Comprehensive heat pump and heating system control:

**NIBE Heat Pump Monitoring**
- All temperature sensors (outdoor, supply, return, hot water, brine circuit)
- Compressor operating statistics (hours, starts)
- Energy consumption breakdown (heating vs. hot water)
- System status and alarm monitoring
- Firmware version tracking

**Thermostat Control**
- Zone-based temperature management
- Scheduled heating programs
- Degree-minutes monitoring for optimal operation
- Automatic control based on room temperature

**Heat Mode Selection**
- Economy, comfort, and vacation modes
- Per-zone temperature setpoints
- Integration with weather forecasts for predictive heating

### Weather Information

Automatic weather data integration via Open-Meteo:

**Current Conditions**
- Temperature at your location
- Precipitation and visibility
- Wind speed and soil temperature

**Daily Forecasts**
- Sunrise and sunset times
- Daylight and sunshine duration
- Rain forecast
- Apparent temperature (feels-like)
- Maximum wind speed

**Hourly Forecasts**
- Detailed temperature predictions
- Precipitation probability
- Wind conditions

Weather data updates automatically every hour and integrates with heating automation for energy-efficient climate control.

### Telegram Bot Control

Control your home remotely via Telegram messenger:

**Available Commands**
- Toggle lights and devices
- Open gates (with confirmation)
- View device status
- Execute scenarios
- Receive notifications for important events

**Features**
- Role-based access control (User, Admin, Super Admin)
- Hierarchical menu navigation
- Status feedback with emoji indicators
- Notification system for alerts and events

**Security**
- Users must be registered and linked to Telegram username
- Permission-based command access
- Admin approval for sensitive actions
- Activity logging for audit trails

### Mobile Access

Access myHAB on any device:

**Progressive Web App (PWA)**
- Install directly from browser (Chrome, Edge, Safari)
- Works offline with cached data
- Native app experience without app store
- Automatic updates when new versions are available

**Responsive Design**
- Optimized layouts for phones, tablets, and desktops
- Touch-friendly controls
- Swipe gestures for navigation

**Installation**
1. Open myHAB in your mobile browser
2. Click "Install" when prompted (or use browser menu)
3. Access from your home screen like a native app

---

## Architecture

### System Overview

myHAB follows a modern three-tier architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────────┐
│                         Clients                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Web App    │  │  Mobile PWA │  │   Telegram Bot          │  │
│  │  (Vue 3)    │  │  (Quasar)   │  │   Remote Control        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└───────────────────────────┬─────────────────────────────────────┘
                            │ GraphQL / WebSocket
┌───────────────────────────▼─────────────────────────────────────┐
│                      Backend Server                              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                   Grails 5 Application                    │   │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────────────┐    │   │
│  │  │  GraphQL   │ │  Services  │ │  Scheduled Jobs    │    │   │
│  │  │    API     │ │   Layer    │ │  (Quartz)          │    │   │
│  │  └────────────┘ └────────────┘ └────────────────────┘    │   │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────────────┐    │   │
│  │  │   Spring   │ │   Event    │ │      MQTT          │    │   │
│  │  │  Security  │ │   System   │ │   Integration      │    │   │
│  │  └────────────┘ └────────────┘ └────────────────────┘    │   │
│  └──────────────────────────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────────┐
│                     External Systems                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────────────────┐ │
│  │PostgreSQL│ │  MQTT    │ │ External │ │    IoT Devices      │ │
│  │ Database │ │ Broker   │ │   APIs   │ │ (ESP32, MEGAD, etc.)│ │
│  └──────────┘ └──────────┘ └──────────┘ └─────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Technology Stack

**Frontend**
| Component | Technology | Purpose |
|-----------|------------|---------|
| Framework | Vue 3 | Reactive UI components |
| UI Library | Quasar Framework | Material Design components |
| API Client | Apollo Client | GraphQL queries and mutations |
| Real-time | WebSocket | Live updates from server |
| Build Tool | Vite/Webpack | Module bundling |
| PWA | Workbox | Service worker and caching |

**Backend**
| Component | Technology | Purpose |
|-----------|------------|---------|
| Framework | Grails 5 (Groovy) | Web application framework |
| API | GraphQL | Flexible data queries |
| ORM | GORM | Database abstraction |
| Security | Spring Security | Authentication and authorization |
| Scheduling | Quartz | Timed job execution |
| Messaging | Spring Integration | MQTT message handling |

**Infrastructure**
| Component | Technology | Purpose |
|-----------|------------|---------|
| Database | PostgreSQL | Persistent data storage |
| Message Broker | Mosquitto (MQTT) | Device communication |
| Caching | In-memory | Performance optimization |

### Communication Layer

**MQTT Protocol**

The system uses MQTT for bidirectional device communication:

- **Device to Server** - Status updates, sensor readings, event notifications
- **Server to Device** - Commands, configuration changes, value updates

Supported device protocols:
| Device Type | Topic Pattern | Description |
|-------------|---------------|-------------|
| ESP32 | `esp/status/{device}`, `esp/port/{device}/{port}` | ESP32 microcontrollers |
| MEGAD | `megad/{device}/port/{port}` | MEGAD-2561 controllers |
| NIBE | `nibe/{device}/status` | Heat pump data |
| Huawei Inverter | `inverter/{device}/{parameter}` | Solar inverter metrics |
| ONVIF Camera | `onvif/{device}/presence` | Camera presence detection |

**GraphQL API**

The GraphQL API provides:
- Flexible queries for entities (devices, ports, zones, users, etc.)
- Mutations for state changes and event publishing
- Subscriptions for real-time updates
- Navigation and breadcrumb support

**WebSocket**

Real-time communication enables:
- Instant UI updates when device states change
- Live dashboard data without page refresh
- Push notifications for alerts and events

### Data Flow

**Sensor Data Collection**

```
Device → MQTT Broker → MqttTopicService → Event System → PortValueService → Database
                                              ↓
                                        WebSocket → UI Update
```

**Command Execution**

```
UI Action → GraphQL Mutation → Service Layer → MqttTopicService → MQTT Broker → Device
                                     ↓
                              Event Publishing → Notification
```

**Scheduled Jobs**

```
Quartz Scheduler → Job Execution → External API (if needed) → MQTT Publishing
                                          ↓
                               Database Update → Statistics
```

### Domain Model

The system is organized around these core entities:

**Infrastructure**
- **Zone** - Physical locations (rooms, floors, buildings)
- **Layer** - Logical groupings (electrical, network, heating)
- **Rack** - Equipment housing
- **Patch Panel** - Cable termination points

**Devices**
- **Device** - Physical controllers (ESP32, MEGAD, virtual devices)
- **DevicePort** - Individual I/O ports on devices
- **DevicePeripheral** - Logical peripherals (lights, sensors, switches)
- **Cable** - Physical cable connections between ports

**Automation**
- **Scenario** - Automation scripts triggered by events
- **Job** - Scheduled tasks with cron triggers
- **EventDefinition** - Event types and subscriptions
- **Configuration** - System and device settings

**Users**
- **User** - System users with credentials
- **Role** - Permission groups (User, Admin, Super Admin)
- **UserRole** - User-role assignments

### Event System

The event-driven architecture enables loose coupling between components:

**Event Flow**
1. Events are published to named topics (e.g., `EVT_LIGHT`, `EVT_GATE`)
2. Subscribers receive events matching their subscriptions
3. Scenarios can be triggered by event patterns
4. Services react to events for state updates

**Event Data Structure**
- `p0` - Event topic name
- `p1` - Entity type (PERIPHERAL, DEVICE, etc.)
- `p2` - Entity ID
- `p3` - Source description
- `p4` - Action or value
- `p5` - Additional metadata (JSON)
- `p6` - User identifier

### Scheduled Tasks

Quartz-based job scheduler handles:

| Job | Interval | Purpose |
|-----|----------|---------|
| Device State Sync | 60s | Synchronize device status via HTTP |
| Port Value Sync | 60s | Trigger MQTT port reads |
| Heating Control | 120s | Thermostat automation |
| Huawei Sync | 120s | Solar inverter data collection |
| NIBE Sync | 60s | Heat pump data collection |
| Meteo Sync | 3600s | Weather data updates |
| Statistics | Hourly/Daily/Monthly | Energy statistics aggregation |
| Auto-Off Timeout | 30s | Automatic device shutdown |
| Event Log Reader | 60s | Process event log queue |

Jobs can be enabled/disabled and intervals adjusted via configuration without code changes.

### Security

**Authentication**
- OAuth 2.0 token-based authentication
- Session management with refresh tokens
- Password encryption using secure hashing

**Authorization**
- Role-based access control (RBAC)
- Permission checks on API endpoints
- Telegram bot command restrictions

**Data Protection**
- Secure credential storage in database
- API keys and tokens in configuration
- HTTPS for all client-server communication

---

## Developer Information

### Project Structure

```
myhab/
├── client/                    # Frontend application
│   └── web-vue3/             # Vue 3 + Quasar web client
│       ├── src/              # Source code
│       ├── src-pwa/          # PWA configuration
│       └── src-capacitor/    # Mobile app wrapper
├── server/                    # Backend application
│   ├── server-core/          # Main Grails application
│   │   ├── grails-app/
│   │   │   ├── controllers/  # REST/GraphQL endpoints
│   │   │   ├── domain/       # GORM domain classes
│   │   │   ├── services/     # Business logic
│   │   │   └── jobs/         # Quartz scheduled jobs
│   │   └── src/main/resources/
│   │       └── schema.graphqls  # GraphQL schema
│   └── server-libs/          # Shared libraries
│       ├── server-config/    # Configuration utilities
│       └── server-rules/     # Business rules engine
├── gradle/                    # Gradle build configuration
└── doc/                       # Documentation
```

### Build System

The project uses Gradle for building:

- `./gradlew build` - Build entire project
- `./gradlew bootRun` - Run backend server
- `npm run serve` - Run frontend development server
- `npm run pwa:build` - Build PWA for production

### Configuration

Key configuration files:
- `server-core/grails-app/conf/application.yml` - Backend settings
- `client/web-vue3/quasar.config.js` - Frontend configuration
- `client/web-vue3/.env` - Environment variables

### Documentation

Detailed documentation is available in the `doc/` folder covering:
- Integration guides (Huawei, NIBE, Meteo, Telegram)
- MQTT service reference
- Quartz job configuration
- Migration guides
- Optimization analyses

---

## License

Copyright (c) myHAB Project

---

*For more information, visit [http://myhab.org/](http://myhab.org/)*
