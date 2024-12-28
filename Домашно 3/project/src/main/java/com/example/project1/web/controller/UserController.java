package com.example.project1.web.controller;

import com.example.project1.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public String addUser(@RequestParam(name = "username") String username,
                          @RequestParam(name = "password") String password,
                          Model model) {
        // Проверка дали корисничкото име или лозинката се празни
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            model.addAttribute("text", "Username and password cannot be empty.");
            return "register-page";
        }

        // Проверка дали корисничкото име содржи нелегални карактери
        if (username.contains(" ") || password.contains(" ")) {
            model.addAttribute("text", "Username and password cannot contain spaces.");
            return "register-page";
        }

        try {
            // Додавање на нов корисник
            userService.addUser(username, password);
            return "redirect:/login"; // Пренасочување кон страницата за логин
        } catch (Exception e) {
            // Ако корисничкото име веќе постои, прикажи порака за грешка
            model.addAttribute("text", "Username already exists.");
            return "register-page"; // Врати ја страницата за регистрација
        }
    }
}
