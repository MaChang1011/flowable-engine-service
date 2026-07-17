package com.example.workflow.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 静态页面路由：将 /designer 重定向到流程设计器
 */
@Controller
public class DesignerController {

    @GetMapping("/designer")
    public String designer() {
        return "redirect:/designer/index.html";
    }
}
