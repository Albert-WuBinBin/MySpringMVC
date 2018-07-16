package com.wbb.service.impl;

import com.wbb.annotation.MyService;
import com.wbb.service.TestService;

@MyService("testServiceImpl")
public class TestServiceImpl implements TestService {

	public String sayHello(String name ,String age) {	
		return "hello,"+name+",age:"+age;
	}

}
