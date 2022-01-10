package eu.devexpert.madhouse.alert

import com.ifountain.opsgenie.client.OpsGenieClient
import com.ifountain.opsgenie.client.swagger.api.AlertApi
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest
import com.ifountain.opsgenie.client.swagger.model.SuccessResponse
import com.ifountain.opsgenie.client.swagger.model.TeamRecipient
import grails.gorm.transactions.Transactional

@Transactional
class AlertService {

    def configProvider

    def sendAlert(Alert alert) {
        OpsGenieClient client = new OpsGenieClient()
        client.setApiKey(configProvider.get(String.class, "opsgenie.apiKey"))
        client.setRootUri(configProvider.get(String.class, "opsgenie.url"))

        AlertApi alertApi = client.alertV2()
        CreateAlertRequest request = new CreateAlertRequest();
        request.setMessage(alert.message);
        request.setAlias(alert.alias);
        request.setDescription(alert.description);
        request.setNote(alert.note);
        request.setTeams(Arrays.asList(new TeamRecipient().name(configProvider.get(String.class, "opsgenie.team")).id(configProvider.get(String.class, "opsgenie.recipientId"))));
        request.setPriority(CreateAlertRequest.PriorityEnum.fromValue(alert.priority.value()));
        request.setUser(configProvider.get(String.class, "opsgenie.email"));

        SuccessResponse response = alertApi.createAlert(request);
        Float took = response.getTook();
        String requestId = response.getRequestId();
        String message = response.getResult();
        log.debug(message)
    }
}

