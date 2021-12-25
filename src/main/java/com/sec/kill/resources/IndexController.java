package com.sec.kill.resources;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/index")
public class IndexController {

    private final AtomicLong counter = new AtomicLong();

    @GetMapping(value = "/greeting")
    public Map<Long, String> greeting(){
        HashMap<Long, String> hashMap = new HashMap<>();
        hashMap.put(counter.incrementAndGet(), "Hello");
        return hashMap;
    }
}
