package com.hari.MedicalPlan.controller;

import com.hari.MedicalPlan.service.MedicationPlanService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        return new ResponseEntity(medicationPlanService.insertIntoKeyStoreAndConvertToMap(new JSONObject(plan)), HttpStatus.OK);
    }

    @DeleteMapping("/delete_plan/{objectId}")
    public ResponseEntity deletePlan(@PathVariable String objectId, @RequestHeader HttpHeaders httpHeaders) {
        return medicationPlanService.deletePlan(objectId, httpHeaders.getIfMatch());
    }

    @PutMapping("/update_plan/{objectId}")
    public ResponseEntity updatePlan(@PathVariable String objectId, @RequestBody String plan, @RequestHeader HttpHeaders httpHeaders) {
        return medicationPlanService.updatePlan(objectId, plan, httpHeaders.getIfMatch());
    }

    @PatchMapping("/update_plan/{objectId}")
    public ResponseEntity updatePlanFields(@PathVariable String objectId, @RequestBody String updatedFields, @RequestHeader HttpHeaders httpHeaders) {
        return medicationPlanService.updatePlanFields(objectId, updatedFields, httpHeaders.getIfMatch());
    }
}
