package com.hhm.service;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class BlackListService {
    Map<String, Date> blackList = new ConcurrentHashMap<>();

    public void addToBlackList(String token, Date expirationDate) {
        blackList.put(token, expirationDate);
    }

    public boolean isBlackListed(String token) {
        Date expirationDate = blackList.get(token);
        if (expirationDate == null) {
            return false;
        }
        // Token naturally expired — no need to keep it in memory; JWT validation rejects it anyway
        if (expirationDate.before(new Date())) {
            blackList.remove(token);
            return false;
        }
        return true;
    }
}
