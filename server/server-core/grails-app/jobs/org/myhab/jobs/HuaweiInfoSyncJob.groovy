package org.myhab.jobs

import grails.gorm.transactions.Transactional
import groovy.json.JsonSlurper
import kong.unirest.HttpResponse
import kong.unirest.JsonNode
import kong.unirest.Unirest
import org.myhab.async.mqtt.MqttTopicService
import org.myhab.config.CfgKey
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType
import org.myhab.domain.device.Device
import org.myhab.domain.device.DeviceModel
import org.myhab.domain.device.DeviceStatus
import org.myhab.domain.device.port.DevicePort
import org.myhab.domain.device.port.PortType
import org.myhab.services.DeviceService
import org.myhab.services.PortValueService
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

import java.util.concurrent.TimeUnit

@DisallowConcurrentExecution
@Transactional
class HuaweiInfoSyncJob implements Job {
    public static final String API_LOGIN_URL = "https://eu5.fusionsolar.huawei.com/thirdData/login"
    public static final String API_STATION_LIST_URL = "https://eu5.fusionsolar.huawei.com/thirdData/getDevRealKpi"
    public static final String STATION_CODE = "NE=36363788"
    public static final String TOKEN_HEADER = "xsrf-token"

    MqttTopicService mqttTopicService
    String token

    static triggers = {
        simple repeatInterval: TimeUnit.SECONDS.toMillis(120)
    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        login()
        sleep(3000)
        readHuaweiDevice(Device.findByModel(DeviceModel.HUAWEI_SUN2000_12KTL_M2), 1, "1000000036363790")
        sleep(3000)
        readHuaweiDevice(Device.findByModel(DeviceModel.ELECTRIC_METER_DTS), 47, "1000000036406276")
    }

    def readHuaweiDevice(Device device, def devTypeId, def devIds) {
        if (device != null) {
            if (token != null) {
                HttpResponse<String> response = Unirest.post(API_STATION_LIST_URL)
                        .header("Content-Type", "application/json")
                        .header("XSRF-TOKEN", token)
                /*.body("{\"stationCodes\" : \"${STATION_CODE}\"}")*/
                        .body("{\"devTypeId\":${devTypeId},\"devIds\" : \"${devIds}\"}")
                        .asString()
                try {
                    def parameters = new JsonSlurper().parseText(response.getBody())
                    if (response.success && parameters["failCode"] != 305 && parameters["success"]) {
                        parameters["data"]?.first()?["dataItemMap"].each { k, v ->
                            if (v != null) {
                                DevicePort port = device.ports.find { it.internalRef == k } as DevicePort
                                if (port == null) {
                                    port = new DevicePort(device: device, type: PortType.SENSOR, internalRef: k, value: v)
                                }
                                mqttTopicService.publishPortValue(device, port, v as String)
                            }
                        }
                        mqttTopicService.publishStatus(device, DeviceStatus.ONLINE)
                    }
                } catch (Exception ex) {
                    ex.printStackTrace()
                }

            }
        }
    }

    void login() {
        def device = Device.findByModel(DeviceModel.HUAWEI_SUN2000_12KTL_M2)
        try {
            def user = device.getConfigurationByKey(CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_USER)
            def passwd = device.getConfigurationByKey(CfgKey.DEVICE.DEVICE_OAUTH_ACCESS_PASSWD)
            if (user && user.value && passwd && passwd.value) {
                HttpResponse<JsonNode> response = Unirest.post(API_LOGIN_URL)
                        .header("Content-Type", "application/json")
                        .body("{\"userName\": \"${user.value}\",\"systemCode\": \"${passwd.value}\"}")
                        .asJson();
                if (response.success) {
                    def tokens = response.getHeaders().get(TOKEN_HEADER)
                    if (tokens != null && !tokens.isEmpty()) {
                        token = tokens.first()
                    }
                } else {
                    log.error(response.status + ": " + response.body?.toPrettyString())
                }
            } else {
                log.error("Username or Password was not configured for Huawei sync")
            }
        } catch (Exception se) {
            log.warn("Can't connect : ${se.message}")
            mqttTopicService.publishStatus(device, DeviceStatus.OFFLINE)
        }

    }
}
