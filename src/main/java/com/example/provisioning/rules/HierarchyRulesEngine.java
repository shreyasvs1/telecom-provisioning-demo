package com.example.provisioning.rules;

import com.example.provisioning.model.WorkOrder;
import com.example.provisioning.model.WorkSpec;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * STEP 5 of the pipeline: organizes the derived work specs into a hierarchy
 * of dispatchable work orders, and decides whether more than one dispatch
 * is needed. In the real system this is IBM ODM again (a second rule set,
 * separate from validation) -- here it's a small, explicit Java stand-in so
 * you can practice explaining "why did this produce 2 work orders instead
 * of 1" the same way you'd explain a rule outcome from ODM.
 *
 * CURRENT RULE (as of this version):
 *   - Work specs are grouped by requiredSkill.
 *   - Each distinct skill group becomes its own dispatchable WorkOrder,
 *     because a single crew can only perform work matching their skill.
 *   - Work orders are sequenced so OUTSIDE_PLANT work always happens before
 *     INSIDE_WIRING or ELECTRONICS work (you can't wire the inside before
 *     the outside connection exists).
 */
@Component
public class HierarchyRulesEngine {

    private static final List<String> SKILL_SEQUENCE = List.of(
            "OUTSIDE_PLANT", "INSIDE_WIRING", "ELECTRONICS"
    );

    public List<WorkOrder> organize(String orderId, List<WorkSpec> workSpecs) {
        Map<String, List<WorkSpec>> bySkill = new LinkedHashMap<>();
        for (WorkSpec spec : workSpecs) {
            bySkill.computeIfAbsent(spec.getRequiredSkill(), k -> new ArrayList<>()).add(spec);
        }

        List<WorkOrder> workOrders = new ArrayList<>();
        int sequence = 1;
        int woCounter = 1;

        // Emit in the defined skill sequence first, so ordering is deterministic
        List<String> orderedSkills = new ArrayList<>(SKILL_SEQUENCE);
        for (String skill : bySkill.keySet()) {
            if (!orderedSkills.contains(skill)) {
                orderedSkills.add(skill); // unknown skills go last, in encounter order
            }
        }

        for (String skill : orderedSkills) {
            List<WorkSpec> specsForSkill = bySkill.get(skill);
            if (specsForSkill == null || specsForSkill.isEmpty()) {
                continue;
            }
            String workOrderId = orderId + "-WO" + woCounter++;
            workOrders.add(new WorkOrder(workOrderId, skill + "_CREW", sequence++, specsForSkill));
        }

        return workOrders;
    }
}
