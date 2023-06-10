package com.hari.MedicalPlan.service;

import com.hari.MedicalPlan.dao.CommonDAO;
import com.hari.MedicalPlan.dao.MedicationPlanDAO;
import com.hari.MedicalPlan.helper.Utility;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class MedicationPlanService {
    @Autowired
    private MedicationPlanDAO planDAO;
    @Autowired
    private CommonDAO commonDAO;
    public ResponseEntity createPlan(String plan) throws Exception {
        JSONObject planObject = new JSONObject(plan);
        try (InputStream inputStream = getClass().getResourceAsStream("/MedicalPlanSchema.json")) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(planObject);
        }

        planDAO.create(planObject, planObject.getString("objectId"));
        String newEtag = Utility.generateHash(plan);
        commonDAO.insertEtag(newEtag);
        HttpHeaders headers = new HttpHeaders();
        headers.setETag("\""+newEtag+"\"");
        return new ResponseEntity(headers, HttpStatus.OK);
    }

    public ResponseEntity getPlan(String id) {
        Object plan = planDAO.getPlan(id);
        if(plan == null) return new ResponseEntity("Plan does not exist", HttpStatus.NOT_FOUND);
        return new ResponseEntity(plan, HttpStatus.OK);
    }

    public ResponseEntity getAllPlans(List<String> etags) {
        String currEtag = commonDAO.getEtag();
        if(currEtag != null) {
            for(String etag: etags) {
                if(currEtag.equals(etag)) return new ResponseEntity("Content not modified", HttpStatus.NOT_MODIFIED);
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setETag("\""+currEtag+"\"");
            return new ResponseEntity(planDAO.getAllPlans(), headers, HttpStatus.OK);
        }
        return new ResponseEntity(planDAO.getAllPlans(), HttpStatus.OK);
    }

    public ResponseEntity deletePlan(String id) {
        Long deleteCount = planDAO.deletePlan(id);
        if(deleteCount == 0) {
            return new ResponseEntity("Plan does not exist", HttpStatus.NOT_FOUND);
        }
        String newEtag = Utility.generateRandom256BitString();
        commonDAO.insertEtag(newEtag);
        HttpHeaders headers = new HttpHeaders();
        headers.setETag("\""+newEtag+"\"");
        return new ResponseEntity(headers, HttpStatus.OK);
    }
}
