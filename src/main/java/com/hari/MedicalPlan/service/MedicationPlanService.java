package com.hari.MedicalPlan.service;

import com.hari.MedicalPlan.config.MessagingConfig;
import com.hari.MedicalPlan.dao.CommonDAO;
import com.hari.MedicalPlan.dao.MedicationPlanDAO;
import com.hari.MedicalPlan.helper.Utility;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.io.InputStream;
import java.util.*;

@Service
public class MedicationPlanService {
    @Autowired
    private MedicationPlanDAO planDAO;
    @Autowired
    private CommonDAO commonDAO;
    @Autowired
    private Jedis jedis;
    @Autowired
    private RabbitTemplate template;


    public ResponseEntity createPlan(String plan) throws Exception {
        JSONObject planObject = new JSONObject(plan);
        try (InputStream inputStream = getClass().getResourceAsStream("/MedicalPlanSchema.json")) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(planObject);
        }

        insertIntoKeyStoreAndConvertToMap(new JSONObject(plan));
        template.convertAndSend(MessagingConfig.MESSAGE_EXCHANGE_NAME, MessagingConfig.ROUTING_KEY, new IndexingMessage("CREATE", new JSONObject(plan).toString()));
        String newEtag = Utility.generateHash(plan);
        commonDAO.insertEtag(newEtag);
        HttpHeaders headers = new HttpHeaders();
        headers.setETag("\""+newEtag+"\"");
        return new ResponseEntity(headers, HttpStatus.OK);
    }

    public ResponseEntity getPlan(String id) {
        String planStoreKey = "plan"+":"+id;
        if(!jedis.exists(planStoreKey)) return new ResponseEntity("Plan does not exist", HttpStatus.NOT_FOUND);
        Object plan = getMap(planStoreKey, new HashMap<>());
        return new ResponseEntity(plan, HttpStatus.OK);
    }

    public ResponseEntity getAllPlans(List<String> etags) {
        String currEtag = commonDAO.getEtag();
        if(currEtag != null) {
            if(etagMatches(etags)) return new ResponseEntity("Content not modified", HttpStatus.NOT_MODIFIED);
            HttpHeaders headers = new HttpHeaders();
            headers.setETag("\""+currEtag+"\"");
            return new ResponseEntity(fetchAllPlans(), headers, HttpStatus.OK);
        }
        List<Map<String, Object>> allPlans = fetchAllPlans();
        String newEtag = Utility.generateHash(allPlans.toString());
        commonDAO.insertEtag(newEtag);
        HttpHeaders headers = new HttpHeaders();
        headers.setETag("\""+newEtag+"\"");
        return new ResponseEntity(allPlans, headers, HttpStatus.OK);
    }

    public ResponseEntity deletePlan(String id, List<String> ifMatchEtag) {
        if(ifMatchEtag == null || ifMatchEtag.size() == 0) {
            return new ResponseEntity("if-match etag is missing", HttpStatus.BAD_REQUEST);
        }

        if(!etagMatches(ifMatchEtag)) {
            return new ResponseEntity("Resource has been modified by another user", HttpStatus.BAD_REQUEST);
        }

        if(!jedis.exists("plan:"+id)) {
            return new ResponseEntity("Plan does not exist", HttpStatus.NOT_FOUND);
        }
        Map<String, Object> oldPlan = getMap("plan:"+id, new HashMap<>());
        template.convertAndSend(MessagingConfig.MESSAGE_EXCHANGE_NAME, MessagingConfig.ROUTING_KEY, new IndexingMessage("DELETE", new JSONObject(oldPlan).toString()));

        deletePlanHelper("plan:"+id, new HashMap<>());
        String newEtag = Utility.generateRandom256BitString();
        commonDAO.insertEtag(newEtag);
        HttpHeaders headers = new HttpHeaders();
        headers.setETag("\""+newEtag+"\"");
        return new ResponseEntity(headers, HttpStatus.OK);
    }

    public ResponseEntity updatePlan(String objectId, String plan, List<String> ifMatchEtag) throws Exception {
        if(ifMatchEtag == null || ifMatchEtag.size() == 0) {
            return new ResponseEntity("if-match etag is missing", HttpStatus.BAD_REQUEST);
        }
        if(!etagMatches(ifMatchEtag)) {
            return new ResponseEntity("Resource has been modified by another user", HttpStatus.BAD_REQUEST);
        }
        if(!jedis.exists("plan:"+objectId)) {
            return new ResponseEntity("Plan does not exist", HttpStatus.NOT_FOUND);
        }

        try (InputStream inputStream = getClass().getResourceAsStream("/MedicalPlanSchema.json")) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(plan);
        }

        deletePlan(objectId, ifMatchEtag);
        Map<String, Map<String, Object>> map = insertIntoKeyStoreAndConvertToMap(new JSONObject(plan));
        template.convertAndSend(MessagingConfig.MESSAGE_EXCHANGE_NAME, MessagingConfig.ROUTING_KEY, new IndexingMessage("CREATE", new JSONObject(plan).toString()));
        String newEtag = Utility.generateHash(fetchAllPlans().toString());
        commonDAO.insertEtag(newEtag);
        HttpHeaders headers = new HttpHeaders();
        headers.setETag("\""+newEtag+"\"");
        return new ResponseEntity(map, headers, HttpStatus.OK);
    }

    private boolean etagMatches(List<String> ifMatchEtag) {
        String currEtag = commonDAO.getEtag();
        for(String etag: ifMatchEtag) {

            if(currEtag.replace("\"", "").equals(etag.replace("\"", ""))) return true;
        }
        return false;
    }


    public ResponseEntity updatePlanFields(String objectId, String updatedFields, List<String> ifMatchEtag) throws Exception {
        if(ifMatchEtag == null || ifMatchEtag.size() == 0) {
            return new ResponseEntity("if-match etag is missing", HttpStatus.BAD_REQUEST);
        }
        if(!etagMatches(ifMatchEtag)) {
            return new ResponseEntity("Resource has been modified by another user", HttpStatus.BAD_REQUEST);
        }

        if(!jedis.exists("plan:"+objectId)) {
            return new ResponseEntity("Plan does not exist", HttpStatus.NOT_FOUND);
        }

        try (InputStream inputStream = getClass().getResourceAsStream("/MedicalPlanSchema.json")) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(new JSONObject(updatedFields));
        }

        Map<String, Map<String, Object>> map = insertIntoKeyStoreAndConvertToMap(new JSONObject(updatedFields));
        template.convertAndSend(MessagingConfig.MESSAGE_EXCHANGE_NAME, MessagingConfig.ROUTING_KEY, new IndexingMessage("CREATE", new JSONObject(updatedFields).toString()));
        String newEtag = Utility.generateHash(fetchAllPlans().toString());
        commonDAO.insertEtag(newEtag);
        HttpHeaders headers = new HttpHeaders();
        headers.setETag("\""+newEtag+"\"");
        return new ResponseEntity(map, headers, HttpStatus.OK);
    }

    public Map<String, Map<String, Object>> insertIntoKeyStoreAndConvertToMap(JSONObject jsonObject) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        Map<String, Object> tempMap = new HashMap<>();
        String storeKey = jsonObject.get("objectType") + ":" + jsonObject.get("objectId");

        for(String key: jsonObject.keySet()) {
            Object valueObject = jsonObject.get(key);

            if(valueObject instanceof JSONObject) {
                Map<String, Map<String, Object>> valueMapObject = insertIntoKeyStoreAndConvertToMap((JSONObject) valueObject);
                jedis.sadd((storeKey +":"+ key), valueMapObject.keySet().iterator().next());
            } else if(valueObject instanceof JSONArray) {
                for(Object item: convertJsonToList((JSONArray) valueObject)) {
                    ((Map<String, Map<String, Object>>) item).keySet().forEach((listKey) -> {
                        jedis.sadd(storeKey+":"+key, listKey);
                    });
                }
            } else {
                jedis.hset(storeKey, key, valueObject.toString());
                tempMap.put(key, valueObject);
            }
            map.put(storeKey, tempMap);
        }
        return map;
    }

    private ArrayList<Map<String, Object>> fetchAllPlans() {
        ArrayList<Map<String, Object>> plans = new ArrayList<>();
        Set<String> allKeys = jedis.keys("*");
        for(String key: allKeys) {
            if(key.split(":").length == 2 && key.split(":")[0].equals("plan")) {
                plans.add(getMap(key, new HashMap<>()));
            }
        }
        return plans;
    }

    public Map<String, Object> getMap(String storeKey, Map<String, Object> resultMap) {
        Set<String> relevantKeys = jedis.keys(storeKey+":*");
        relevantKeys.add(storeKey);

        for(String key: relevantKeys) {
            if (key.equals(storeKey)) {
                Map<String, String> valueObject = jedis.hgetAll(key);
                for (String attributeKey : valueObject.keySet()) {
                    if (!attributeKey.equalsIgnoreCase("eTag")) {
                        resultMap.put(attributeKey, isInteger(valueObject.get(attributeKey)) ? Integer.parseInt(valueObject.get(attributeKey)) : valueObject.get(attributeKey));
                    }
                }
                continue;
            }
            String childNodeKey = key.substring((storeKey + ":").length());
            Set<String> members = jedis.smembers(key);
            if (members.size() > 1 || childNodeKey.equals("linkedPlanServices")) {
                List<Object> listObject = new ArrayList<>();
                for (String member : members) {
                    Map<String, Object> listMap = new HashMap<>();
                    listObject.add(getMap(member, listMap));
                }
                resultMap.put(childNodeKey, listObject);
            } else {
                Map<String, String> childObjects = jedis.hgetAll(members.iterator().next());
                Map<String, Object> childMap = new HashMap<>();
                for (String attributeKey : childObjects.keySet()) {
                    childMap.put(attributeKey, isInteger(childObjects.get(attributeKey)) ? Integer.parseInt(childObjects.get(attributeKey)) : childObjects.get(attributeKey));
                }
                resultMap.put(childNodeKey, childMap);
            }
        }
        return resultMap;
    }


    private Map<String, Object> deletePlanHelper(String redisKey, Map<String, Object> resultMap) {
        Set<String> keys = jedis.keys(redisKey + ":*");
        keys.add(redisKey);

        for (String key : keys) {
            if (key.equals(redisKey)) {
                jedis.del(new String[]{key});
            } else {
                String newKey = key.substring((redisKey + ":").length());
                Set<String> members = jedis.smembers(key);
                if (members.size() > 1 || newKey.equals("linkedPlanServices")) {
                    List<Object> listObj = new ArrayList<>();
                    for (String member : members) {
                        deletePlanHelper(member, null);
                    }
                    jedis.del(new String[]{key});
                } else {
                    jedis.del(new String[]{members.iterator().next(), key});
                }
            }
        }
        return resultMap;
    }


    public List<Object> convertJsonToList(JSONArray jsonArray) {
        List<Object> jsonObjectList = new ArrayList<>();
        for (Object item : jsonArray) {
            if (item instanceof JSONArray) item = convertJsonToList((JSONArray) item);
            else if (item instanceof JSONObject) item = insertIntoKeyStoreAndConvertToMap((JSONObject) item);
            jsonObjectList.add(item);
        }
        return jsonObjectList;
    }

    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
