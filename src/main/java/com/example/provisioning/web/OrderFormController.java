package com.example.provisioning.web;

import com.example.provisioning.exception.ValidationException;
import com.example.provisioning.model.ProvisioningResponse;
import com.example.provisioning.service.OrderIdGenerator;
import com.example.provisioning.service.ProvisioningOrchestrator;
import com.example.provisioning.validation.RequestValidator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * The order entry screen: a server-rendered form that lets an operator enter
 * an order without writing JSON. It runs the same pipeline as the JSON
 * endpoint by calling ProvisioningOrchestrator directly; the JSON endpoint in
 * ProvisioningController is not involved and is unchanged.
 */
@Controller
public class OrderFormController {

    private static final String FORM_VIEW = "order-form";

    private final ProvisioningOrchestrator orchestrator;
    private final OrderIdGenerator orderIdGenerator;

    public OrderFormController(ProvisioningOrchestrator orchestrator, OrderIdGenerator orderIdGenerator) {
        this.orchestrator = orchestrator;
        this.orderIdGenerator = orderIdGenerator;
    }

    @ModelAttribute("requestTypes")
    public List<String> requestTypes() {
        return RequestValidator.VALID_REQUEST_TYPES;
    }

    @ModelAttribute("serviceCodes")
    public List<String> serviceCodes() {
        return RequestValidator.VALID_SERVICE_CODES;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/orders/new")
    public String newOrder(Model model) {
        model.addAttribute("orderForm", new OrderForm());
        return FORM_VIEW;
    }

    @PostMapping("/orders")
    public String submit(@ModelAttribute("orderForm") OrderForm form, Model model, RedirectAttributes redirect) {
        String orderId = orderIdGenerator.nextOrderId();
        try {
            ProvisioningResponse response = orchestrator.process(form.toRequest(orderId));
            // Redirect so a browser refresh does not submit the order again
            redirect.addFlashAttribute("submittedOrderId", orderId);
            redirect.addFlashAttribute("workOrderCount", response.getTotalDispatchableWorkOrders());
            return "redirect:/orders/new";
        } catch (ValidationException e) {
            model.addAttribute("rejectedOrderId", orderId);
            model.addAttribute("violations", e.getRuleViolations());
            return FORM_VIEW;
        } catch (RuntimeException e) {
            model.addAttribute("failedOrderId", orderId);
            return FORM_VIEW;
        }
    }
}
