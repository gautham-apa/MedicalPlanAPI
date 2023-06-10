package com.hari.MedicalPlan.dao;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CommonDAO {
    @Autowired
    private RedisTemplate redisTemplate;
    private final String etagKey = "etag";

    public void insertEtag(String etagValue) {
        redisTemplate.opsForHash().put(etagKey, etagKey, etagValue);
    }

    public String getEtag() {
        return (String) redisTemplate.opsForHash().get(etagKey, etagKey);
    }
}
