package com.wbb.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.wbb.annotation.MyAutowired;
import com.wbb.annotation.MyController;
import com.wbb.annotation.MyRequestMapping;
import com.wbb.annotation.MyRequestParam;
import com.wbb.service.TestService;

@MyController
@MyRequestMapping("/test")
public class TestController {

	@MyAutowired("testServiceImpl")
	private TestService testService;
	
	@MyRequestMapping("/hello")
	public String sayHello(HttpServletRequest request ,HttpServletResponse response,
			@MyRequestParam("name")String name,@MyRequestParam("age")String age) {
		try {
			PrintWriter pw = response.getWriter();
			String sayHello = testService.sayHello(name,age);
			pw.write(sayHello);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return null;
	}
	
}
