package com.sec.kill.resources;

import com.sec.kill.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class CustomerController {

    CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/md5")
    public String getMD5(Integer id, Long uid) {
        String md5;
        try {
            md5 = customerService.getMd5(id, uid);
        } catch (Exception e) {
            e.printStackTrace();
            return uid + " - 生成失败：" + e.getMessage();
        }
        return "md5 信息为：" + md5;
    }
}
