package com.wbb.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.wbb.annotation.MyAutowired;
import com.wbb.annotation.MyController;
import com.wbb.annotation.MyRequestMapping;
import com.wbb.annotation.MyRequestParam;
import com.wbb.annotation.MyService;
import com.wbb.controller.TestController;

public class DispatcherServlet extends HttpServlet{
	

	private static final long serialVersionUID = 1L;
	
	List<String> packageNames = new ArrayList<String>();//所有的类名
	Map<String, Object> instanceMap = new HashMap<String, Object>();//name-全类名  存放实例化类
	Map<String, Object> handlelMap = new HashMap<String, Object>();//路径-controller 存放路径controller
	
	public DispatcherServlet() {
		super();
	}
	public void init(ServletConfig config) throws ServletException {
		// 包扫描,获取包中的文件,得到文件夹下的所有类名
		scanPackage("com.wbb");
		for (String string : packageNames) {
			System.out.println(string);
		}
		doInstance();
		System.out.println("---实例化---");
		for (Entry<String, Object> entry: instanceMap.entrySet()) {
			System.out.println(entry.getKey()+"-"+entry.getValue());
		}
		doIoc();
		System.out.println("---注入---");
		doMapping();//路径映射到controller
		for (Entry<String, Object> entry: handlelMap.entrySet()) {
			System.out.println(entry.getKey()+"-"+entry.getValue());
		}
	}

	private void doMapping() {
		for (Entry<String, Object> entry: instanceMap.entrySet()) {
			Object instance = entry.getValue();
			Class<?> clazz = instance.getClass();
			if(clazz.isAnnotationPresent(MyRequestMapping.class)) {
				MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
				String classPath = requestMapping.value();
				Method[] methods = clazz.getMethods();
				for (Method method : methods) {
					if(method.isAnnotationPresent(MyRequestMapping.class)) {
						MyRequestMapping requestMapping2 = method.getAnnotation(MyRequestMapping.class);
						String methodPath = requestMapping2.value();
						handlelMap.put(classPath+methodPath, method);
					}
				}
			}else {
				continue;
			}
		}
	}
	private void doIoc() {
		for (Entry<String, Object> entry: instanceMap.entrySet()) {
			Object instance = entry.getValue();
			Class<?> clazz = instance.getClass();
			if(clazz.isAnnotationPresent(MyController.class)) {
				Field[] fields = clazz.getDeclaredFields();
				for (Field field : fields) {
					if(field.isAnnotationPresent(MyAutowired.class)) {
						MyAutowired autowired = field.getAnnotation(MyAutowired.class);
						String value = autowired.value();
						field.setAccessible(true);
						try {
							field.set(instance, instanceMap.get(value));
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
					}else {
						continue;
					}
				}
			}	
		}	
	}
	private void doInstance() {
		for (String className : packageNames) {
			try {
				Class<?> clazz = Class.forName(className);
				if(clazz.isAnnotationPresent(MyController.class)) {
					Object newInstance = clazz.newInstance();
					MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
					String value = requestMapping.value();
					instanceMap.put(value, newInstance);
				}else if(clazz.isAnnotationPresent(MyService.class)) {
					Object newInstance = clazz.newInstance();
					MyService myService = clazz.getAnnotation(MyService.class);
					String value = myService.value();
					instanceMap.put(value, newInstance);
				}
				else {
					continue;
				}		 
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
	}
	private void scanPackage(String packageName) {
		URL url = this.getClass().getClassLoader().getResource("/"+packageName.replaceAll("\\.", "/"));
		String fileName = url.getFile();
		File file = new File(fileName);
		File[] fileList = file.listFiles();
		for (File eachFile : fileList) {
			if(eachFile.isDirectory()) {
				scanPackage(packageName+"."+eachFile.getName());
			}
			else {
				packageNames.add(packageName+"."+eachFile.getName().replace(".class", ""));
			}
		}
		
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String servletPath = req.getServletPath();
		Method method = (Method) handlelMap.get(servletPath);
		TestController testController = (TestController) instanceMap.get("/"+servletPath.split("/")[1]);
		Object[] args = getArgs(req, resp, method);
		try {
			method.invoke(testController, args);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static Object[] getArgs(HttpServletRequest request ,
			HttpServletResponse response ,Method method) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		Object[] args = new Object[parameterTypes.length];
		int index = 0;
		for (Class<?> clazz : parameterTypes) {
			if(ServletRequest.class.isAssignableFrom(clazz)) {
				args[index] = request;
			}
			if(ServletResponse.class.isAssignableFrom(clazz)) {
				args[index] = response;
			}
			Annotation[] paramAns = method.getParameterAnnotations()[index];
			if(paramAns.length>0) {
				for (Annotation annotation : paramAns) {
					if(MyRequestParam.class.isAssignableFrom(annotation.getClass())) {
						MyRequestParam rp = (MyRequestParam) annotation;
						args[index] = request.getParameter(rp.value());
					}
				}
			}
			index++;
		}	
		return args;
	}
}
