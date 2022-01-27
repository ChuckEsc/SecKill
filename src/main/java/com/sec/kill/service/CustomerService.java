package com.sec.kill.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    StockService stockService;

    public String getMd5(Integer id, Long uid) {

        if (id == null || uid == null)
            throw new RuntimeException("商品id或用户id不合法！！");

        // TODO: 验证用户id合法性（略）——> 查询数据库
        // 验证商品id合法性（略）——> 查询数据库

        String key = "MS_KEY_" + id + "_" + uid;// MS_KEY_商品id_用户id
        String salt = "!!!Q*?...#";
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            String from = System.currentTimeMillis() + salt;
            value = DigestUtils.md5DigestAsHex(from.getBytes(StandardCharsets.UTF_8));//时间戳 + salt
        }
        stringRedisTemplate.opsForValue().set(key, value, 30, TimeUnit.SECONDS);//刷新验证值超时时间
        log.info("用户验证值获取：用户{}，商品{}, md5{}", uid, id, value);
        return value;
    }


}
