package eu.devexpert.madhouse.async.mqtt

import eu.devexpert.madhouse.async.mqtt.handlers.MQTTMessageHandler
import groovy.util.logging.Slf4j
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.annotation.ServiceActivator
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory
import org.springframework.integration.mqtt.core.MqttPahoClientFactory
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler

import javax.transaction.Transactional

@Slf4j
@EnableIntegration
@IntegrationComponentScan
@Configuration
public class MQTTConfiguration {

    @Value('${mqtt.hostname}')
    String hostname
    @Value('${mqtt.port}')
    String port
    @Value('${mqtt.username}')
    String username
    @Value('${mqtt.password}')
    String password
    @Value('${mqtt.topics}')
    String topics

    @Bean
    MqttPahoClientFactory mqttClientFactory() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(["tcp://${hostname}:${port}"] as String[]);

        if (username != null && !username.isEmpty()) {
            options.setUserName(username);
        }

        if (password != null && !password.isEmpty()) {
            options.setPassword(password.toCharArray());
        }

        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);

        return factory;
    }

    @Bean
    @Transactional
    IntegrationFlow mqttInbound(MqttPahoClientFactory mqttClientFactory
                                , MQTTMessageHandler mQTTMessageHandler) {
        def topics = topics.split(",")

        return IntegrationFlows
                .from(new MqttPahoMessageDrivenChannelAdapter(MqttAsyncClient.generateClientId(), mqttClientFactory, topics as String[]))
                .handle(mQTTMessageHandler).get();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    MessageHandler mqttOutbound() {
        MqttPahoMessageHandler mqttPahoMessageHandler = new MqttPahoMessageHandler(MqttAsyncClient.generateClientId(), mqttClientFactory());
        mqttPahoMessageHandler.setAsync(true);
        mqttPahoMessageHandler.setDefaultQos(1);
        mqttPahoMessageHandler.setDefaultTopic(MQTTTopic.COMMON.DEFAULT.regex);
        return mqttPahoMessageHandler;
    }

    @Bean
    MessageChannel mqttOutboundChannel() {
        DirectChannel directChannel = new DirectChannel();
        return directChannel;
    }

}