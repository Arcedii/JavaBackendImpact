package com.impact.lessons.controllers;
import com.impact.lessons.services.HelloService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    private final HelloService helloService = new HelloService();

    @GetMapping("/")
    public String sayHello(){
        return helloService.sayHallo();
    }

}
