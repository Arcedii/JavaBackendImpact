package com.impact.lessons.services;
import com.impact.lessons.constants.AppConstant;

public class HelloService {

    public HelloService(){

        AppConstant appConstant = new AppConstant();
    }

    public String sayHallo()
    {
        return AppConstant.HelloWorld;
    }
}


