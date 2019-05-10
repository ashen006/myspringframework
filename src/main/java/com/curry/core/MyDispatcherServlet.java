package com.curry.core;

import com.curry.annotation.MyAutowrited;
import com.curry.annotation.MyController;
import com.curry.annotation.MyQualifier;
import com.curry.annotation.MyRequestMapping;
import com.curry.annotation.MyService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author: Curry
 * @Date: 2019/4/21 11:07
 */
public class MyDispatcherServlet extends HttpServlet {


    /**
     * 存储所有class文件名
     */
    private List<String> clazzList = new ArrayList<String>();

    /**
     * 存储实例化后的bean对象，key为请求名
     */
    private Map<String,Object> instanceMap = new HashMap<String, Object>();
    /**
     * 存储请求映射与调用方法对象
     */
    private Map<String,Object> requestMappingMap = new HashMap<String, Object>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /**
         * 处理请求，调用具体的Controller
         * 1.处理请求URL
         */
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String url = requestURI.replace(contextPath,"");
        /**
         * 2.从存储请求映射与调用方法对象的集合中获取应调用的具体方法
         */
        Method requestMethod = (Method) requestMappingMap.get(url);
        /**
         * 获取具体的调用实例(即依赖注入的bean实例)
         */
        Object beanObject = instanceMap.get("/"+url.split("/")[1]);
        try {
            /**
             * 通过反射激活实例方法，即具体实例的方法调用
             */
            requestMethod.invoke(beanObject);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void init() throws ServletException {
        /**
         * 扫描指定目录下的class文件
         */
        scanAllAnno("com.curry");
        /**
         * 利用反射及注解实现bean的实例化
         */
        beanInit();
        /**
         * 利用反射及注解实现依赖注入
         */
        beanIoc();
        /**
         * 创建请求与类的映射关系
         */
        requestMapping();
        System.out.println(requestMappingMap);
    }

    /**
     * 映射
     */
    private void requestMapping() {
        if (instanceMap!=null && instanceMap.size()>0) {
            for (Map.Entry<String, Object> entry : instanceMap.entrySet()) {
                /**
                 * 获取实例的Class对象
                 */
                Class<? extends Object> instanceClzss = entry.getValue().getClass();
                /**
                 * 判断controller中的请求注解，其他非controller没有请求映射
                 */
                if (instanceClzss.isAnnotationPresent(MyController.class)){
                    /**
                     * 通过class对象获取类中的所有方法
                     */
                    Method[] allMethods = instanceClzss.getDeclaredMethods();
                    for (Method method:allMethods){
                        /**
                         * 判断是否存在MyRequestMapping注解
                         */
                        if (method.isAnnotationPresent(MyRequestMapping.class)){
                            MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                            /**
                             * 拼接请求URL，并放入map中
                             */
                            String requestUrl = entry.getKey()+requestMapping.value();
                            requestMappingMap.put(requestUrl,method);
                        }
                    }

                }
            }
        }
    }

    /**
     * IOC
     */
    private void beanIoc() {
        if (instanceMap!=null && instanceMap.size()>0){
            for (Map.Entry<String,Object> entry:instanceMap.entrySet()){
                /**
                 * 通过实例获取其所有的属性,并判断属性上是否有MyAutowrited注解，根据MyQualifier进行注入
                 */
                Field[] fields = entry.getValue().getClass().getDeclaredFields();
                for (Field field:fields){
                    if (field.isAnnotationPresent(MyAutowrited.class)){
                        MyQualifier myQualifier = field.getAnnotation(MyQualifier.class);
                        /**
                         * 打破私有化限制
                         */
                        field.setAccessible(true);
                        try {
                            /**
                             * 实现注入
                             */
                            field.set(entry.getValue(),instanceMap.get(myQualifier.value()));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * 实例化bean
     */
    private void beanInit() {
        if (clazzList!=null && clazzList.size()>0){
            for (String clzName:clazzList){
                try {
                    Class<?> clzss = Class.forName(clzName);
                    /**
                     * 判断类上的注解,并将实例化的bean存放到map中
                     */
                    if (clzss.isAnnotationPresent(MyController.class)){
                        MyRequestMapping mapping = clzss.getAnnotation(MyRequestMapping.class);
                        String key = mapping.value();
                        instanceMap.put(key,clzss.newInstance());
                    }else if (clzss.isAnnotationPresent(MyService.class)){
                        MyService service = clzss.getAnnotation(MyService.class);
                        String key = service.value();
                        instanceMap.put(key,clzss.newInstance());
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 扫描所有class文件
     * @param packageName 根目录
     */
    private void scanAllAnno(String packageName) {
        /**
         * 获取扫描文件的全路径
         */
        String path = this.getClass().getClassLoader()
                .getResource("/"+packageName.replaceAll("\\.","/"))
                .getFile();
        System.out.println(path);
        /**
         * 遍历路径下的所有class文件,将类名全路径存储到集合中
         */
        File files = new File(path);
        String[] filenames = files.list();
        for (String filename:filenames){
            /**
             * 生成文件时需要添加全路径
             */
            File file = new File(path,filename);
            //判断是文件夹还是文件
            if (file.isDirectory()){
                scanAllAnno(packageName+"."+filename);
            }else{
                if (filename.indexOf(".class")>0){
                    clazzList.add(packageName+"."+filename.replace(".class",""));
                }
            }
        }
    }
}
