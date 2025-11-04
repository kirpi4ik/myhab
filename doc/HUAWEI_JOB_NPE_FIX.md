# Huawei Job NullPointerException Fix

## Issue

NullPointerException occurring in `HuaweiInfoSyncJob` when trying to publish device status:

```
java.lang.NullPointerException: Cannot get property 'model' on null object
at org.myhab.async.mqtt.MqttTopicService.publishStatus(MqttTopicService.groovy:135)
at org.myhab.jobs.HuaweiInfoSyncJob.login(HuaweiInfoSyncJob.groovy:104)
```

## Root Cause Analysis

### Problem 1: Null Device Object

In `HuaweiInfoSyncJob.groovy` (lines 45, 47):

```groovy
readHuaweiDevice(Device.findByModel(DeviceModel.HUAWEI_SUN2000_12KTL_M2), 1, "1000000036363790")
readHuaweiDevice(Device.findByModel(DeviceModel.ELECTRIC_METER_DTS), 47, "1000000036406276")
```

**Issue**: `Device.findByModel()` can return `null` if no device with that model exists in the database.

### Problem 2: Null Device Model

Even if a device exists, its `model` property could be `null`.

### Problem 3: Unsupported Device Model

In `MqttTopicService.groovy` (lines 69-86), the `device()` method:

```groovy
DeviceTopic device(model) {
    switch (model) {
        case DeviceModel.MEGAD_2561_RTC:
            return new MQTTTopic.MEGA()
        case DeviceModel.ESP32:
            return new MQTTTopic.ESP()
        // ... other cases
        case DeviceModel.TMEZON_INTERCOM:
            break  // Returns null!
    }
    // No default case - returns null for unsupported models
}
```

**Issue**: If the model doesn't match any case or hits a `break` statement, the method returns `null`.

### Problem 4: No Null Check in publishStatus

In `MqttTopicService.groovy` (line 135):

```groovy
def publishStatus(Device d, DeviceStatus status) {
    mqttPublishGateway.sendToMqtt(new SimpleTemplateEngine()
            .createTemplate(device(d.model).topicByType(STATUS_WRITE))  // NPE here!
            .make([map: new MQTTMessage(deviceCode: d.code)]).toString(), status.name().toLowerCase())
}
```

**Issue**: No null checks for:
1. `d` (device object)
2. `d.model` (device model property)
3. `device(d.model)` return value

## Solutions Implemented

### Fix 1: Add Null Checks in MqttTopicService

**File**: `server/server-core/grails-app/services/org/myhab/async/mqtt/MqttTopicService.groovy`

```groovy
def publishStatus(Device d, DeviceStatus status) {
    // Check if device or model is null
    if (!d?.model) {
        log.warn("Cannot publish status for device ${d?.code}: device model is null")
        return
    }
    
    // Get device topic and check if supported
    def deviceTopic = device(d.model)
    if (!deviceTopic) {
        log.warn("Cannot publish status for device ${d.code}: unsupported device model ${d.model}")
        return
    }
    
    // Proceed with publishing
    mqttPublishGateway.sendToMqtt(new SimpleTemplateEngine()
            .createTemplate(deviceTopic.topicByType(STATUS_WRITE))
            .make([map: new MQTTMessage(deviceCode: d.code)]).toString(), status.name().toLowerCase())
}
```

**Changes**:
1. Added null check for device and model: `if (!d?.model)`
2. Store `device()` result in variable before using it
3. Added null check for device topic: `if (!deviceTopic)`
4. Added warning logs for both failure cases
5. Early return to prevent NPE

### Fix 2: Add Null Checks in HuaweiInfoSyncJob

**File**: `server/server-core/grails-app/jobs/org/myhab/jobs/HuaweiInfoSyncJob.groovy`

```groovy
@Override
void execute(JobExecutionContext context) throws JobExecutionException {
    login()
    sleep(3000)
    
    // Check if inverter device exists before using it
    def inverterDevice = Device.findByModel(DeviceModel.HUAWEI_SUN2000_12KTL_M2)
    if (inverterDevice) {
        readHuaweiDevice(inverterDevice, 1, "1000000036363790")
    } else {
        log.warn("Huawei inverter device not found (model: ${DeviceModel.HUAWEI_SUN2000_12KTL_M2})")
    }
    
    sleep(3000)
    
    // Check if meter device exists before using it
    def meterDevice = Device.findByModel(DeviceModel.ELECTRIC_METER_DTS)
    if (meterDevice) {
        readHuaweiDevice(meterDevice, 47, "1000000036406276")
    } else {
        log.warn("Electric meter device not found (model: ${DeviceModel.ELECTRIC_METER_DTS})")
    }
}
```

**Changes**:
1. Store `Device.findByModel()` result in variable
2. Check if device exists before calling `readHuaweiDevice()`
3. Added warning logs when devices are not found
4. Prevents passing null device to downstream methods

## Benefits

### 1. Prevents NPE
- No more crashes when device or model is null
- No more crashes when device model is unsupported

### 2. Better Error Logging
- Clear warning messages indicate the problem
- Logs include device code and model for debugging
- Helps identify missing or misconfigured devices

### 3. Graceful Degradation
- Job continues to run even if one device is missing
- Other devices can still be processed
- System remains operational

### 4. Easier Troubleshooting
- Warning logs point directly to the problem
- No need to analyze stack traces
- Clear indication of what needs to be fixed

## Testing

### Test Case 1: Device Not Found
**Scenario**: No device with `HUAWEI_SUN2000_12KTL_M2` model exists

**Expected**:
```
WARN - Huawei inverter device not found (model: HUAWEI_SUN2000_12KTL_M2)
```

**Result**: Job continues, no NPE

### Test Case 2: Device Model is Null
**Scenario**: Device exists but `model` property is null

**Expected**:
```
WARN - Cannot publish status for device <code>: device model is null
```

**Result**: Method returns early, no NPE

### Test Case 3: Unsupported Device Model
**Scenario**: Device has a model that's not in the switch statement

**Expected**:
```
WARN - Cannot publish status for device <code>: unsupported device model <model>
```

**Result**: Method returns early, no NPE

### Test Case 4: Valid Device
**Scenario**: Device exists with supported model

**Expected**: Status published successfully to MQTT

**Result**: Normal operation

## Recommendations

### 1. Database Integrity
Ensure all devices have a valid model:

```sql
-- Find devices without a model
SELECT * FROM device WHERE model IS NULL;

-- Update devices with missing model
UPDATE device SET model = 'APPROPRIATE_MODEL' WHERE id = ?;
```

### 2. Device Configuration
Create missing devices in the system:
- Huawei inverter: `HUAWEI_SUN2000_12KTL_M2`
- Electric meter: `ELECTRIC_METER_DTS`

### 3. Job Configuration
Consider making device lookup more flexible:
- Use device code instead of model for lookup
- Add configuration for device IDs
- Make device lookup optional with configuration

### 4. Monitoring
Add monitoring for warning logs:
- Alert when devices are not found
- Alert when unsupported models are encountered
- Track frequency of null checks being triggered

## Related Code

### Other Methods with Similar Pattern

The `topic()` method in `MqttTopicService` already has proper null checking:

```groovy
String topic(Device d, MQTTMessage mqttMessage) {
    def topic = device(d.model)
    if (topic != null) {
        new SimpleTemplateEngine().createTemplate(topic.topicByType(WRITE_SINGLE_VAL))
            .make([map: mqttMessage]).toString()
    } else {
        log.error("Invalid device model: ${d.model}")
    }
}
```

This pattern should be applied consistently across all methods that call `device()`.

## Files Modified

1. `server/server-core/grails-app/services/org/myhab/async/mqtt/MqttTopicService.groovy`
   - Added null checks in `publishStatus()` method
   - Added warning logs for failure cases

2. `server/server-core/grails-app/jobs/org/myhab/jobs/HuaweiInfoSyncJob.groovy`
   - Added null checks before calling `readHuaweiDevice()`
   - Added warning logs when devices are not found

## Conclusion

The NPE has been fixed by adding proper null checks at multiple levels:
1. In the job before passing devices to methods
2. In the service before using device properties
3. In the service before calling methods on potentially null objects

The fix ensures the system continues to operate even when devices are missing or misconfigured, while providing clear logging to help identify and resolve the underlying issues.

