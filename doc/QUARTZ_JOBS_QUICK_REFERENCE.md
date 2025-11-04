# Quartz Jobs - Quick Reference Card

## Configuration Location
`server/server-core/grails-app/conf/application.yml`

## Quick Syntax
```yaml
quartz:
  jobs:
    <jobName>:
      enabled: true/false
      interval: <seconds>
```

## All Jobs at a Glance

| Job Name | Config Key | Default | Default Interval | Purpose |
|----------|-----------|---------|------------------|---------|
| HuaweiInfoSyncJob | `huaweiInfoSync` | ❌ false | 120s | Huawei solar inverter sync |
| NibeInfoSyncJob | `nibeInfoSync` | ✅ true | 60s | Nibe heat pump data sync |
| NibeTokenRefreshJob | `nibeTokenRefresh` | ✅ true | 500s | Nibe OAuth token refresh |
| DeviceControllerStateSyncJob | `deviceControllerStateSync` | ✅ true | 60s | Device HTTP sync |
| HeatingControlJob | `heatingControl` | ✅ true | 120s | Thermostat control |
| ConfigSyncJob | `configSync` | ✅ true | 60s | Config synchronization |
| PortValueSyncTriggerJob | `portValueSyncTrigger` | ✅ true | 60s | MQTT port read trigger |
| SwitchOFFOnTimeoutJob | `switchOffOnTimeout` | ✅ true | 30s | Auto-off on timeout |
| RandomColorsJob | `randomColors` | ❌ false | 5s | Demo: Random RGB colors |
| EventLogReaderJob | `eventLogReader` | ✅ true | 60s | Event log processing |
| ElectricityMetricStatistics1HJob | `electricityMetricStatistics1H` | ✅ true | 3600s | Hourly electricity stats |
| ElectricityMetricStatistics24HJob | `electricityMetricStatistics24H` | ✅ true | 86400s | Daily electricity stats |
| ElectricityMetricStatistics1MonthJob | `electricityMetricStatistics1Month` | ✅ true | 2592000s | Monthly electricity stats |

## Common Operations

### Disable a Job
```yaml
quartz:
  jobs:
    jobName:
      enabled: false
```

### Enable a Job
```yaml
quartz:
  jobs:
    jobName:
      enabled: true
```

### Change Interval
```yaml
quartz:
  jobs:
    jobName:
      enabled: true
      interval: 300  # 5 minutes
```

## Interval Conversion

| Time | Seconds |
|------|---------|
| 30 seconds | 30 |
| 1 minute | 60 |
| 2 minutes | 120 |
| 5 minutes | 300 |
| 10 minutes | 600 |
| 30 minutes | 1800 |
| 1 hour | 3600 |
| 2 hours | 7200 |
| 6 hours | 21600 |
| 12 hours | 43200 |
| 24 hours | 86400 |
| 1 week | 604800 |
| 30 days | 2592000 |

## Startup Log Messages

### Job Enabled
```
<JobName>: ENABLED - Registering trigger with interval Xs
```

### Job Disabled
```
<JobName>: DISABLED - Not registering trigger
```

## Runtime Log Messages

### Job Skipped (Disabled)
```
INFO: <JobName> is DISABLED via configuration, skipping execution
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Job still runs when disabled | Check config location (before `---`), restart app |
| Job doesn't start when enabled | Verify `enabled: true` (boolean), check startup logs |
| Interval not applied | Restart application, check startup logs |
| Configuration not found | Check YAML syntax, indentation |

## Example Configurations

### Development (Minimal Jobs)
```yaml
quartz:
  jobs:
    huaweiInfoSync:
      enabled: false
    nibeInfoSync:
      enabled: false
    deviceControllerStateSync:
      enabled: true
      interval: 120  # Slower polling
    heatingControl:
      enabled: false  # No heating in dev
```

### Production (All Jobs)
```yaml
quartz:
  jobs:
    huaweiInfoSync:
      enabled: true
      interval: 120
    nibeInfoSync:
      enabled: true
      interval: 60
    deviceControllerStateSync:
      enabled: true
      interval: 60
    heatingControl:
      enabled: true
      interval: 120
```

### Testing (Demo Jobs Enabled)
```yaml
quartz:
  jobs:
    randomColors:
      enabled: true
      interval: 5
```

## Quick Checklist

- [ ] Configuration in first YAML document (before `---`)
- [ ] Correct indentation (2 spaces)
- [ ] Boolean values (`true`/`false`, not `yes`/`no`)
- [ ] Interval in seconds
- [ ] Application restarted after changes
- [ ] Startup logs checked
- [ ] Job execution monitored

## Need More Info?

📖 **Full Guide:** `QUARTZ_JOBS_CONFIGURATION_GUIDE.md`  
📋 **Summary:** `QUARTZ_JOBS_CONFIGURATION_SUMMARY.md`  
🔧 **HuaweiJob Details:** `QUARTZ_JOB_CONFIGURATION_FINAL.md`

