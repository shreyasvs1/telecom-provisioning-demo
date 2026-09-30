package com.example.provisioning;

import com.example.provisioning.model.Address;
import com.example.provisioning.model.ProvisioningRequest;
import com.example.provisioning.model.ProvisioningResponse;
import com.example.provisioning.service.ProvisioningOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProvisioningOrchestratorTest {

    @Autowired
    private ProvisioningOrchestrator orchestrator;

    @Test
    void copperPhoneOnly_producesSingleWorkOrder() {
        ProvisioningRequest request = new ProvisioningRequest();
        request.setOrderId("TEST-1");
        request.setCustomerId("CUST-1");
        request.setRequestType("NEW_INSTALL");
        request.setRequestedServices(List.of("PHONE"));

        Address address = new Address();
        address.setStreet("1 Test St");
        address.setCity("Testville");
        address.setState("IL");
        address.setZip("62701"); // odd -> COPPER

        request.setServiceAddress(address);

        ProvisioningResponse response = orchestrator.process(request);

        assertEquals(1, response.getTotalDispatchableWorkOrders());
    }

    @Test
    void fiberBundle_producesMultipleWorkOrders() {
        ProvisioningRequest request = new ProvisioningRequest();
        request.setOrderId("TEST-2");
        request.setCustomerId("CUST-2");
        request.setRequestType("NEW_INSTALL");
        request.setRequestedServices(List.of("INTERNET", "PHONE", "TV"));

        Address address = new Address();
        address.setStreet("2 Test St");
        address.setCity("Testville");
        address.setState("IL");
        address.setZip("62702"); // even -> FIBER

        request.setServiceAddress(address);

        ProvisioningResponse response = orchestrator.process(request);

        assertTrue(response.getTotalDispatchableWorkOrders() > 1,
                "Bundled fiber order across 3 services should require more than one dispatchable work order");
    }
}
