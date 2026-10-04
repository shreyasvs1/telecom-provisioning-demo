package com.example.provisioning.web;

import com.example.provisioning.model.Address;
import com.example.provisioning.model.ProvisioningRequest;
import com.example.provisioning.persistence.ProvisioningRequestRecord;
import com.example.provisioning.persistence.ProvisioningRequestRepository;
import com.example.provisioning.persistence.RequestStatus;
import com.example.provisioning.service.OrderIdGenerator;
import com.example.provisioning.service.ProvisioningOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderFormControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProvisioningRequestRepository requestRepository;
    @Autowired private ProvisioningOrchestrator orchestrator;
    @Autowired private OrderIdGenerator orderIdGenerator;

    @Test
    void homePage_linksToNewOrder() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/orders/new\"")));
    }

    @Test
    void newOrderPage_showsEveryOrderAttributeWithItsChoices() throws Exception {
        mockMvc.perform(get("/orders/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("order-form"))
                .andExpect(content().string(containsString("name=\"customerId\"")))
                .andExpect(content().string(containsString("name=\"requestType\"")))
                .andExpect(content().string(containsString("<option value=\"NEW_INSTALL\">")))
                .andExpect(content().string(containsString("<option value=\"UPGRADE\">")))
                .andExpect(content().string(containsString("<option value=\"TRANSFER\">")))
                .andExpect(content().string(containsString("name=\"street\"")))
                .andExpect(content().string(containsString("name=\"city\"")))
                .andExpect(content().string(containsString("name=\"state\"")))
                .andExpect(content().string(containsString("name=\"zip\"")))
                .andExpect(content().string(containsString("name=\"requestedServices\"")))
                .andExpect(content().string(containsString("value=\"INTERNET\"")))
                .andExpect(content().string(containsString("value=\"PHONE\"")))
                .andExpect(content().string(containsString("value=\"TV\"")));
    }

    @Test
    void newOrderPage_hasNoOrderIdField() throws Exception {
        mockMvc.perform(get("/orders/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("orderId"))));
    }

    @Test
    void newOrderPage_hasSubmitAndCancel() throws Exception {
        mockMvc.perform(get("/orders/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("type=\"submit\"")))
                .andExpect(content().string(containsString("<a class=\"button\" href=\"/\">Cancel</a>")));
    }

    @Test
    void twoOrdersSubmittedInARow_receiveDifferentGeneratedOrderIds() throws Exception {
        String first = submitValidOrder();
        String second = submitValidOrder();

        assertTrue(first.matches("ORD-\\d+"), first);
        assertTrue(second.matches("ORD-\\d+"), second);
        assertNotEquals(first, second);

        for (String orderId : List.of(first, second)) {
            List<ProvisioningRequestRecord> saved = requestRepository.findByOrderIdOrderByIdAsc(orderId);
            assertEquals(1, saved.size());
            assertEquals(RequestStatus.COMPLETED, saved.get(0).getStatus());
            assertEquals("CUST-500", saved.get(0).getCustomerId());
        }
    }

    @Test
    void generatedOrderId_skipsAnIdAlreadyUsedThroughTheJsonEndpoint() {
        long current = numberOf(orderIdGenerator.nextOrderId());
        String handWritten = "ORD-" + (current + 1);
        orchestrator.process(request(handWritten));

        assertNotEquals(handWritten, orderIdGenerator.nextOrderId());
        assertEquals(1, requestRepository.findByOrderIdOrderByIdAsc(handWritten).size());
    }

    @Test
    void rejectedOrder_showsTheViolationsAndKeepsTheEnteredValues() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerId", "CUST-502")
                        .param("requestType", "NEW_INSTALL")
                        .param("street", "1 Remote Rd")
                        .param("city", "Nowhere")
                        .param("state", "MT")
                        .param("zip", "99999")
                        .param("requestedServices", "INTERNET"))
                .andExpect(status().isOk())
                .andExpect(view().name("order-form"))
                .andExpect(content().string(containsString("RULE_LOCATION_NOT_SERVICEABLE")))
                .andExpect(content().string(containsString("value=\"1 Remote Rd\"")));
    }

    @Test
    void emptyForm_isRejectedWithoutAnErrorPage() throws Exception {
        mockMvc.perform(post("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("order-form"))
                .andExpect(content().string(containsString("RULE_CUSTOMER_ID_REQUIRED")))
                .andExpect(content().string(containsString("RULE_SERVICES_REQUIRED")));
    }

    private String submitValidOrder() throws Exception {
        MvcResult result = mockMvc.perform(post("/orders")
                        .param("customerId", "CUST-500")
                        .param("requestType", "NEW_INSTALL")
                        .param("street", "123 Main St")
                        .param("city", "Springfield")
                        .param("state", "IL")
                        .param("zip", "62701")
                        .param("requestedServices", "PHONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/new"))
                .andExpect(flash().attributeExists("submittedOrderId"))
                .andExpect(flash().attribute("workOrderCount", 1))
                .andReturn();
        return (String) result.getFlashMap().get("submittedOrderId");
    }

    private long numberOf(String orderId) {
        return Long.parseLong(orderId.substring("ORD-".length()));
    }

    private ProvisioningRequest request(String orderId) {
        Address address = new Address();
        address.setStreet("123 Main St");
        address.setCity("Springfield");
        address.setState("IL");
        address.setZip("62701");

        ProvisioningRequest request = new ProvisioningRequest();
        request.setOrderId(orderId);
        request.setCustomerId("CUST-500");
        request.setRequestType("NEW_INSTALL");
        request.setServiceAddress(address);
        request.setRequestedServices(List.of("PHONE"));
        return request;
    }
}
