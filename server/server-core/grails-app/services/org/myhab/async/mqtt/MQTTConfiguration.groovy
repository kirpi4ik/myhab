package org.myhab.async.mqtt

import org.myhab.config.ConfigProvider
import org.myhab.async.mqtt.handlers.MQTTMessageHandler
import groovy.util.logging.Slf4j
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory
import org.springframework.integration.mqtt.core.MqttPahoClientFactory
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler

@Slf4j
@EnableIntegration
@IntegrationComponentScan
@Configuration
public class MQTTConfiguration {

    @Autowired
    ConfigProvider configProvider

    @Bean
    MqttPahoClientFactory mqttClientFactory() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(["tcp://${configProvider.get(String.class, "mqtt.hostname")}:${configProvider.get(String.class, "mqtt.port")}"] as String[]);
        options.setUserName(configProvider.get(String.class, "mqtt.username"));
        options.setPassword(configProvider.get(String.class, "mqtt.password").toCharArray());
        // Reconnect after broker restarts; keep the session so QoS1 messages
        // published while we were away (device state echoes) are delivered on
        // reconnect instead of silently lost. Requires stable client ids.
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);

        return factory;
    }

    @Bean
    IntegrationFlow mqttInbound(MqttPahoClientFactory mqttClientFactory
                                , MQTTMessageHandler mQTTMessageHandler) {
        def topics = configProvider.get(String.class, "mqtt.topics").split(",")

        // Stable client id — a random id would make the persistent session
        // (cleanSession=false) useless and leak dead sessions on the broker.
        return IntegrationFlow
                .from(new MqttPahoMessageDrivenChannelAdapter("myhab-server-inbound", mqttClientFactory, topics as String[]))
                .handle(mQTTMessageHandler).get();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    MessageHandler mqttOutbound() {
        MqttPahoMessageHandler mqttPahoMessageHandler = new MqttPahoMessageHandler("myhab-server-outbound", mqttClientFactory());
        mqttPahoMessageHandler.setAsync(true);
        mqttPahoMessageHandler.setDefaultQos(1);
        mqttPahoMessageHandler.setDefaultTopic(MQTTTopic.COMMON.topic(DeviceTopic.TopicTypes.LISTEN));
        return mqttPahoMessageHandler;
    }

    @Bean
    MessageChannel mqttOutboundChannel() {
        DirectChannel directChannel = new DirectChannel();
        return directChannel;
    }

}