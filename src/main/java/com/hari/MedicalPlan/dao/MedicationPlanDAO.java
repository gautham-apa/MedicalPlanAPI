package com.hari.MedicalPlan.dao;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MedicationPlanDAO {
    @Autowired
    private RedisTemplate redisTemplate;
    private final String key = "MedicalPlan";

    public void create(JSONObject plan, String id) {
        redisTemplate.opsForHash().put(key, id, plan.toMap());
    }

    public List<Object> getAllPlans() {
        List<Object> objs =  redisTemplate.opsForHash().values(key);
        return objs;
    }

    public Object getPlan(String id) {
        Object plan = redisTemplate.opsForHash().get(key, id);
        return plan;
    }

    public Long deletePlan(String id) {
        Long deleteCount = redisTemplate.opsForHash().delete(key, id);
        return deleteCount;
    }
}
