package eu.devexpert.madhouse.alert

import com.ifountain.opsgenie.client.OpsGenieClient
import com.ifountain.opsgenie.client.swagger.api.AlertApi
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest
import com.ifountain.opsgenie.client.swagger.model.SuccessResponse
import com.ifountain.opsgenie.client.swagger.model.TeamRecipient
import grails.gorm.transactions.Transactional

@Transactional
class AlertService {

    def sendAlert(Alert alert) {
        OpsGenieClient client = new OpsGenieClient()
        client.setApiKey("***REMOVED***")
        client.setRootUri("***REMOVED***")

        AlertApi alertApi = client.alertV2()
        CreateAlertRequest request = new CreateAlertRequest();
        request.setMessage(alert.message);
        request.setAlias(alert.alias);
        request.setDescription(alert.description);
        request.setNote(alert.note);
        request.setTeams(Arrays.asList(new TeamRecipient().name("***REMOVED***").id("***REMOVED***")));
        request.setPriority(CreateAlertRequest.PriorityEnum.fromValue(alert.priority.value()));
        request.setUser("dumitru.ciubenco@gmail.com");

        SuccessResponse response = alertApi.createAlert(request);
        Float took = response.getTook();
        String requestId = response.getRequestId();
        String message = response.getResult();
        log.debug(message)
    }
}

