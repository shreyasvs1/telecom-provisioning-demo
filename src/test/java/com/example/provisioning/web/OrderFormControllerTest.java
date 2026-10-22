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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesRegex;
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
    void newOrderPage_offersFindAddressWithoutSubmittingTheForm() throws Exception {
        mockMvc.perform(get("/orders/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<label for=\"zip\">Zip / Postal Code</label>")))
                .andExpect(content().string(containsString("<button type=\"button\" class=\"button\" id=\"find-address\">Find address</button>")))
                // Hidden in the markup; only JavaScript reveals it
                .andExpect(content().string(containsString("id=\"address-lookup\" hidden")))
                .andExpect(content().string(containsString("src=\"/js/address-lookup.js\"")))
                .andExpect(content().string(containsString("src=\"/js/address-form.js\"")))
                .andExpect(content().string(containsString("https://www.openstreetmap.org/copyright")));
    }

    @Test
    void addressLookupScripts_areServed() throws Exception {
        mockMvc.perform(get("/js/address-lookup.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("nominatim.openstreetmap.org")));
        mockMvc.perform(get("/js/address-form.js"))
                .andExpect(status().isOk());
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

    // KAN-6 AC1 and AC5
    @Test
    void emptyForm_showsAMessageForEachFieldAndCreatesNoOrder() throws Exception {
        long before = requestRepository.count();

        mockMvc.perform(post("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("order-form"))
                .andExpect(model().attributeHasFieldErrors("orderForm",
                        "customerId", "requestType", "street", "city", "state", "zip", "requestedServices"))
                .andExpect(content().string(containsString("Enter the customer ID.")))
                .andExpect(content().string(containsString("Choose a request type.")))
                .andExpect(content().string(containsString("Enter the street.")))
                .andExpect(content().string(containsString("Enter the city.")))
                .andExpect(content().string(containsString("Enter the state or province.")))
                .andExpect(content().string(containsString("Enter the zip or postal code.")))
                .andExpect(content().string(containsString("Choose at least one service.")))
                .andExpect(content().string(not(containsString("was rejected"))));

        assertEquals(before, requestRepository.count());
    }

    // KAN-6 AC2
    @Test
    void oneAddressFieldMissing_showsAMessageForThatFieldOnly() throws Exception {
        mockMvc.perform(order("zip", ""))
                .andExpect(status().isOk())
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrors("orderForm", "zip"))
                .andExpect(content().string(containsString("Enter the zip or postal code.")))
                .andExpect(content().string(not(containsString("Enter the street."))))
                .andExpect(content().string(not(containsString("Enter the city."))))
                .andExpect(content().string(not(containsString("Enter the state or province."))));
    }

    // KAN-6 AC3
    @Test
    void noServiceSelected_showsAMessageAtTheServicesField() throws Exception {
        mockMvc.perform(order("requestedServices", null))
                .andExpect(status().isOk())
                .andExpect(model().errorCount(1))
                .andExpect(model().attributeHasFieldErrors("orderForm", "requestedServices"))
                .andExpect(content().string(containsString("Choose at least one service.")));
    }

    // KAN-6 AC4
    @Test
    void failedSubmission_keepsTheEnteredValues() throws Exception {
        mockMvc.perform(post("/orders")
                        .param("customerId", "CUST-600")
                        .param("requestType", "UPGRADE")
                        .param("street", "9 Elm St")
                        .param("city", "")
                        .param("state", "IL")
                        .param("zip", "62701")
                        .param("requestedServices", "INTERNET"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("orderForm", "city"))
                .andExpect(content().string(containsString("value=\"CUST-600\"")))
                .andExpect(content().string(containsString("<option value=\"UPGRADE\" selected=\"selected\">")))
                .andExpect(content().string(containsString("value=\"9 Elm St\"")))
                .andExpect(content().string(containsString("value=\"62701\"")))
                .andExpect(content().string(matchesRegex("(?s).*<input type=\"checkbox\" value=\"INTERNET\"[^>]*checked=\"checked\".*")));
    }

    @Test
    void unknownRequestTypeOrService_isReportedAgainstItsField() throws Exception {
        mockMvc.perform(order("requestType", "CANCEL_EVERYTHING", "requestedServices", "RADIO"))
                .andExpect(status().isOk())
                .andExpect(model().errorCount(2))
                .andExpect(model().attributeHasFieldErrors("orderForm", "requestType", "requestedServices"))
                .andExpect(content().string(containsString("Choose one of NEW_INSTALL, UPGRADE, TRANSFER.")))
                .andExpect(content().string(containsString("is not a service that can be ordered.")));
    }

    // KAN-6 AC6
    @Test
    void validForm_passesTheFieldChecksAndIsProcessed() throws Exception {
        long before = requestRepository.count();

        mockMvc.perform(order())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/new"))
                .andExpect(flash().attributeExists("submittedOrderId"));

        assertEquals(before + 1, requestRepository.count());
    }

    /** A valid order, with optional name/value overrides. A null value leaves that field out entirely. */
    private MockHttpServletRequestBuilder order(String... overrides) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("customerId", "CUST-500");
        fields.put("requestType", "NEW_INSTALL");
        fields.put("street", "123 Main St");
        fields.put("city", "Springfield");
        fields.put("state", "IL");
        fields.put("zip", "62701");
        fields.put("requestedServices", "PHONE");
        for (int i = 0; i < overrides.length; i += 2) {
            fields.put(overrides[i], overrides[i + 1]);
        }
        MockHttpServletRequestBuilder request = post("/orders");
        fields.forEach((name, value) -> {
            if (value != null) {
                request.param(name, value);
            }
        });
        return request;
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
