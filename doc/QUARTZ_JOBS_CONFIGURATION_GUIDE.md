# Quartz Jobs Configuration Guide

## Date: November 4, 2025

## Overview

All Quartz jobs in the MyHab application can now be enabled/disabled and have their intervals configured via `application.yml`. This provides centralized control over job execution without code changes.

## Configuration Structure

### Location
`server/server-core/grails-app/conf/application.yml`

### Format
```yaml
quartz:
  pluginEnabled: true
  autoStartup: true
  purgeQuartzTablesOnStartup: false
  threadPool:
    class: "org.quartz.simpl.SimpleThreadPool"
    threadCount: 25
    threadPriority: 5
  jobs:
    <jobName>:
      enabled: true/false
      interval: <seconds>
```

## Available Jobs

### 1. Huawei Solar Inverter Sync
**Job:** `HuaweiInfoSyncJob`  
**Config Key:** `quartz.jobs.huaweiInfoSync`  
**Default:** Disabled  
**Default Interval:** 120 seconds (2 minutes)

```yaml
quartz:
  jobs:
    huaweiInfoSync:
      enabled: false
      interval: 120
```

**Purpose:** Synchronizes data from Huawei solar inverter devices.

---

### 2. Nibe Heat Pump Data Sync
**Job:** `NibeInfoSyncJob`  
**Config Key:** `quartz.jobs.nibeInfoSync`  
**Default:** Enabled  
**Default Interval:** 60 seconds (1 minute)

```yaml
quartz:
  jobs:
    nibeInfoSync:
      enabled: true
      interval: 60
```

**Purpose:** Reads and synchronizes data from Nibe heat pump via API.

---

### 3. Nibe OAuth Token Refresh
**Job:** `NibeTokenRefreshJob`  
**Config Key:** `quartz.jobs.nibeTokenRefresh`  
**Default:** Enabled  
**Default Interval:** 500 seconds (~8 minutes)

```yaml
quartz:
  jobs:
    nibeTokenRefresh:
      enabled: true
      interval: 500
```

**Purpose:** Refreshes OAuth tokens for Nibe API authentication.

---

### 4. Device Controller State Sync
**Job:** `DeviceControllerStateSyncJob`  
**Config Key:** `quartz.jobs.deviceControllerStateSync`  
**Default:** Enabled  
**Default Interval:** 60 seconds (1 minute)

```yaml
quartz:
  jobs:
    deviceControllerStateSync:
      enabled: true
      interval: 60
```

**Purpose:** Synchronizes device controller states via HTTP polling.

---

### 5. Heating/Thermostat Control
**Job:** `HeatingControlJob`  
**Config Key:** `quartz.jobs.heatingControl`  
**Default:** Enabled  
**Default Interval:** 120 seconds (2 minutes)

```yaml
quartz:
  jobs:
    heatingControl:
      enabled: true
      interval: 120
```

**Purpose:** Controls heating actuators based on temperature sensors and schedules.

---

### 6. Configuration Synchronization
**Job:** `ConfigSyncJob`  
**Config Key:** `quartz.jobs.configSync`  
**Default:** Enabled  
**Default Interval:** 60 seconds (1 minute)

```yaml
quartz:
  jobs:
    configSync:
      enabled: true
      interval: 60
```

**Purpose:** Synchronizes configuration from database to in-memory cache.

---

### 7. Port Value Sync Trigger
**Job:** `PortValueSyncTriggerJob`  
**Config Key:** `quartz.jobs.portValueSyncTrigger`  
**Default:** Enabled  
**Default Interval:** 60 seconds (1 minute)

```yaml
quartz:
  jobs:
    portValueSyncTrigger:
      enabled: true
      interval: 60
```

**Purpose:** Triggers MQTT read requests for device port values.

---

### 8. Switch OFF on Timeout
**Job:** `SwitchOFFOnTimeoutJob`  
**Config Key:** `quartz.jobs.switchOffOnTimeout`  
**Default:** Enabled  
**Default Interval:** 30 seconds

```yaml
quartz:
  jobs:
    switchOffOnTimeout:
      enabled: true
      interval: 30
```

**Purpose:** Automatically switches off peripherals after configured timeout.

---

### 9. Random Colors (Demo/Testing)
**Job:** `RandomColorsJob`  
**Config Key:** `quartz.jobs.randomColors`  
**Default:** **Disabled** (demo job)  
**Default Interval:** 5 seconds

```yaml
quartz:
  jobs:
    randomColors:
      enabled: false
      interval: 5
```

**Purpose:** Demo job that randomly changes RGB light colors.

---

### 10. Event Log Reader
**Job:** `EventLogReaderJob`  
**Config Key:** `quartz.jobs.eventLogReader`  
**Default:** Enabled  
**Default Interval:** 60 seconds (1 minute)

```yaml
quartz:
  jobs:
    eventLogReader:
      enabled: true
      interval: 60
```

**Purpose:** Reads and processes event logs (currently mostly commented out).

---

### 11. Electricity Metric Statistics - 1 Hour
**Job:** `ElectricityMetricStatistics1HJob`  
**Config Key:** `quartz.jobs.electricityMetricStatistics1H`  
**Default:** Enabled  
**Default Interval:** 3600 seconds (1 hour)

```yaml
quartz:
  jobs:
    electricityMetricStatistics1H:
      enabled: true
      interval: 3600
```

**Purpose:** Aggregates electricity metrics for 1-hour periods.

---

### 12. Electricity Metric Statistics - 24 Hours
**Job:** `ElectricityMetricStatistics24HJob`  
**Config Key:** `quartz.jobs.electricityMetricStatistics24H`  
**Default:** Enabled  
**Default Interval:** 86400 seconds (24 hours)

```yaml
quartz:
  jobs:
    electricityMetricStatistics24H:
      enabled: true
      interval: 86400
```

**Purpose:** Aggregates electricity metrics for 24-hour periods.

---

### 13. Electricity Metric Statistics - 1 Month
**Job:** `ElectricityMetricStatistics1MonthJob`  
**Config Key:** `quartz.jobs.electricityMetricStatistics1Month`  
**Default:** Enabled  
**Default Interval:** 2592000 seconds (30 days)

```yaml
quartz:
  jobs:
    electricityMetricStatistics1Month:
      enabled: true
      interval: 2592000
```

**Purpose:** Aggregates electricity metrics for monthly periods.

---

## Implementation Details

### Dual-Layer Protection

Each job implements two layers of control:

#### Layer 1: Startup (Trigger Registration)
```groovy
static triggers = {
    def config = Holders.grailsApplication?.config
    def enabled = config?.getProperty('quartz.jobs.<jobName>.enabled', Boolean)
    def interval = config?.getProperty('quartz.jobs.<jobName>.interval', Integer) ?: <default>
    
    if (enabled == null) {
        enabled = <default>  // true or false
    }
    
    if (enabled) {
        println "<JobName>: ENABLED - Registering trigger with interval ${interval}s"
        simple repeatInterval: TimeUnit.SECONDS.toMillis(interval)
    } else {
        println "<JobName>: DISABLED - Not registering trigger"
    }
}
```

**Purpose:** Prevents trigger registration if job is disabled, saving resources.

#### Layer 2: Runtime Check
```groovy
@Override
void execute(JobExecutionContext context) throws JobExecutionException {
    def config = Holders.grailsApplication?.config
    def enabled = config?.getProperty('quartz.jobs.<jobName>.enabled', Boolean)
    
    if (enabled == null) {
        enabled = <default>
    }
    
    if (!enabled) {
        log.info("<JobName> is DISABLED via configuration, skipping execution")
        return
    }
    
    // Job logic...
}
```

**Purpose:** Provides additional safety layer and allows dynamic disabling.

### Default Values

| Job Type | Default Enabled | Rationale |
|----------|----------------|-----------|
| **Production Jobs** | `true` | Backward compatibility, expected to run |
| **Demo/Test Jobs** | `false` | Should be explicitly enabled |
| **Optional Integration Jobs** | `false` | May not be applicable to all installations |

### Configuration Reading

**Correct Approach:**
```groovy
def enabled = config?.getProperty('quartz.jobs.jobName.enabled', Boolean)

if (enabled == null) {
    enabled = false  // or true, depending on desired default
}
```

**Why No Default in getProperty():**
- Allows distinguishing between "not configured" and "explicitly set to default"
- Provides explicit control over default behavior
- Prevents accidental execution when configuration is missing

## Usage Examples

### Example 1: Disable a Job
```yaml
quartz:
  jobs:
    huaweiInfoSync:
      enabled: false
```

**Result:** Job will not register trigger, will not execute.

### Example 2: Enable a Job with Custom Interval
```yaml
quartz:
  jobs:
    nibeInfoSync:
      enabled: true
      interval: 300  # 5 minutes instead of default 60 seconds
```

**Result:** Job executes every 5 minutes.

### Example 3: Enable Demo Job for Testing
```yaml
quartz:
  jobs:
    randomColors:
      enabled: true
      interval: 10  # Every 10 seconds
```

**Result:** Random colors job runs every 10 seconds.

## Startup Output

When the application starts, you'll see output like:

```
HuaweiInfoSyncJob: DISABLED - Not registering trigger
NibeInfoSyncJob: ENABLED - Registering trigger with interval 60s
NibeTokenRefreshJob: ENABLED - Registering trigger with interval 500s
DeviceControllerStateSyncJob: ENABLED - Registering trigger with interval 60s
HeatingControlJob: ENABLED - Registering trigger with interval 120s
ConfigSyncJob: ENABLED - Registering trigger with interval 60s
PortValueSyncTriggerJob: ENABLED - Registering trigger with interval 60s
SwitchOFFOnTimeoutJob: ENABLED - Registering trigger with interval 30s
RandomColorsJob: DISABLED - Not registering trigger
EventLogReaderJob: ENABLED - Registering trigger with interval 60s
ElectricityMetricStatistics1HJob: ENABLED - Registering trigger with interval 3600s
ElectricityMetricStatistics24HJob: ENABLED - Registering trigger with interval 86400s
ElectricityMetricStatistics1MonthJob: ENABLED - Registering trigger with interval 2592000s
```

## Runtime Logs

When a disabled job would have executed (if trigger was registered), you'll see:

```
INFO: <JobName> is DISABLED via configuration, skipping execution
```

## Troubleshooting

### Job Still Runs When Disabled

**Check 1:** Verify configuration location
```yaml
# Must be in FIRST YAML document, BEFORE any ---
quartz:
  jobs:
    jobName:
      enabled: false
```

**Check 2:** Restart application
Configuration is read at startup only.

**Check 3:** Check startup logs
Look for: `<JobName>: DISABLED - Not registering trigger`

### Job Doesn't Start When Enabled

**Check 1:** Verify boolean value
```yaml
enabled: true  # Not "yes", "1", or other values
```

**Check 2:** Check startup logs
Look for: `<JobName>: ENABLED - Registering trigger with interval Xs`

**Check 3:** Check for errors in logs
Look for exceptions during job initialization.

### Interval Not Applied

**Check 1:** Verify interval is in seconds
```yaml
interval: 300  # 5 minutes = 300 seconds
```

**Check 2:** Restart application
Interval changes require restart.

**Check 3:** Check startup logs
Look for: `Registering trigger with interval <value>s`

## Best Practices

### 1. Environment-Specific Configuration

Use Spring profiles for different environments:

```yaml
# Development
---
environments:
  development:
    quartz:
      jobs:
        nibeInfoSync:
          enabled: false  # Disable in dev if no API access

# Production
---
environments:
  production:
    quartz:
      jobs:
        nibeInfoSync:
          enabled: true
```

### 2. Disable Unused Jobs

If your installation doesn't use certain integrations, disable them:

```yaml
quartz:
  jobs:
    huaweiInfoSync:
      enabled: false  # No Huawei inverter
    nibeInfoSync:
      enabled: false  # No Nibe heat pump
```

### 3. Adjust Intervals for Performance

For high-frequency jobs, increase intervals if system is under load:

```yaml
quartz:
  jobs:
    deviceControllerStateSync:
      interval: 120  # Increase from 60 to 120 seconds
```

### 4. Enable Demo Jobs Only When Needed

```yaml
quartz:
  jobs:
    randomColors:
      enabled: false  # Keep disabled in production
```

### 5. Monitor Job Execution

Check logs regularly for:
- Job execution warnings/errors
- Performance issues
- Unexpected behavior

## Migration from Hardcoded Intervals

### Before
```groovy
static triggers = {
    simple repeatInterval: TimeUnit.SECONDS.toMillis(60)
}
```

### After
```groovy
static triggers = {
    def config = Holders.grailsApplication?.config
    def enabled = config?.getProperty('quartz.jobs.jobName.enabled', Boolean)
    def interval = config?.getProperty('quartz.jobs.jobName.interval', Integer) ?: 60
    
    if (enabled == null) {
        enabled = true
    }
    
    if (enabled) {
        println "JobName: ENABLED - Registering trigger with interval ${interval}s"
        simple repeatInterval: TimeUnit.SECONDS.toMillis(interval)
    } else {
        println "JobName: DISABLED - Not registering trigger"
    }
}

@Override
void execute(JobExecutionContext context) throws JobExecutionException {
    def config = Holders.grailsApplication?.config
    def enabled = config?.getProperty('quartz.jobs.jobName.enabled', Boolean)
    
    if (enabled == null) {
        enabled = true
    }
    
    if (!enabled) {
        log.info("JobName is DISABLED via configuration, skipping execution")
        return
    }
    
    // Job logic...
}
```

## Summary

### Benefits

✅ **Centralized Control:** All job configuration in one place  
✅ **No Code Changes:** Enable/disable jobs via configuration  
✅ **Environment-Specific:** Different settings per environment  
✅ **Performance Tuning:** Adjust intervals without code changes  
✅ **Safe Defaults:** Explicit control over default behavior  
✅ **Dual-Layer Protection:** Startup + runtime checks  
✅ **Clear Logging:** Startup and runtime status messages  

### Files Modified

1. **`application.yml`** - Added configuration for all 13 jobs
2. **`HuaweiInfoSyncJob.groovy`** - Added configuration support
3. **`NibeInfoSyncJob.groovy`** - Added configuration support
4. **`NibeTokenRefreshJob.groovy`** - Added configuration support
5. **`DeviceControllerStateSyncJob.groovy`** - Added configuration support
6. **`HeatingControlJob.groovy`** - Added configuration support
7. **`ConfigSyncJob.groovy`** - Added configuration support
8. **`PortValueSyncTriggerJob.groovy`** - Added configuration support
9. **`SwitchOFFOnTimeoutJob.groovy`** - Added configuration support
10. **`RandomColorsJob.groovy`** - Added configuration support
11. **`EventLogReaderJob.groovy`** - Added configuration support
12. **`ElectricityMetricStatistics1HJob.groovy`** - Added configuration support
13. **`ElectricityMetricStatistics24HJob.groovy`** - Added configuration support
14. **`ElectricityMetricStatistics1MonthJob.groovy`** - Added configuration support

### Total Jobs Configured

**13 Quartz Jobs** now support configuration-based enable/disable and interval control.

---

**Ready for Production:** ✅  
**Backward Compatible:** ✅  
**Fully Documented:** ✅

