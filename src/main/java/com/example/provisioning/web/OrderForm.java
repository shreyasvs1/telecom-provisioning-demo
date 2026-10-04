package com.example.provisioning.web;

import com.example.provisioning.model.Address;
import com.example.provisioning.model.ProvisioningRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * What the operator fills in on the "New order" page. It carries the same
 * attributes as ProvisioningRequest except orderId, which the application
 * generates on submit.
 */
public class OrderForm {

    private String customerId;
    private String requestType;
    private String street;
    private String city;
    private String state;
    private String zip;
    private List<String> requestedServices = new ArrayList<>();

    public ProvisioningRequest toRequest(String orderId) {
        Address address = new Address();
        address.setStreet(street);
        address.setCity(city);
        address.setState(state);
        address.setZip(zip);

        ProvisioningRequest request = new ProvisioningRequest();
        request.setOrderId(orderId);
        request.setCustomerId(customerId);
        request.setRequestType(requestType);
        request.setServiceAddress(address);
        request.setRequestedServices(requestedServices);
        return request;
    }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }

    public List<String> getRequestedServices() { return requestedServices; }
    public void setRequestedServices(List<String> requestedServices) { this.requestedServices = requestedServices; }
}
