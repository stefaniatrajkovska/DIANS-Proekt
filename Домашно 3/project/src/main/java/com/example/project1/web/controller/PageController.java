package com.example.project1.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/register")
    public String getRegisterPage() {
        return "register-page";
    }

    @GetMapping("/login")
    public String getLoginPage() {
        return "login-page";
    }

}
