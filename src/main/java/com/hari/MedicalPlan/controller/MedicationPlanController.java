package com.hari.MedicalPlan.controller;

import com.hari.MedicalPlan.service.MedicationPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MedicationPlanController {
    @Autowired
    private MedicationPlanService medicationPlanService;

    @GetMapping("/all_plans")
    public ResponseEntity getAllPlans(@RequestHeader HttpHeaders headers) {
        List<String> etags = headers.getIfNoneMatch();
        return medicationPlanService.getAllPlans(etags);
    }

    @GetMapping("/plan/{objectId}")
    public ResponseEntity getPlan(@PathVariable String objectId) {
        return medicationPlanService.getPlan(objectId);
    }

    @PostMapping("/create_plan")
    public ResponseEntity createPlan(@RequestBody String plan) throws Exception {
        return medicationPlanService.createPlan(plan);
    }

    @DeleteMapping("/delete_plan/{objectId}")
    public ResponseEntity deletePlan(@PathVariable String objectId) {
        return medicationPlanService.deletePlan(objectId);
    }
}
