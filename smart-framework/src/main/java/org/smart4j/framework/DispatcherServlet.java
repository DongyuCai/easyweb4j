package org.smart4j.framework;

import org.smart4j.framework.bean.Data;
import org.smart4j.framework.bean.Handler;
import org.smart4j.framework.bean.Param;
import org.smart4j.framework.bean.View;
import org.smart4j.framework.helper.*;
import org.smart4j.framework.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求转发器
 * Created by CaiDongYu on 2016/4/11.
 */
@WebServlet(urlPatterns = "/*" , loadOnStartup = 0)
public class DispatcherServlet extends HttpServlet{

    @Override
    public void init(ServletConfig servletConfig) throws ServletException{
        //获取 ServletContext 对象（用于注册servlet）
        ServletContext servletContext = servletConfig.getServletContext();
        //注册处理JSP的Servlet
        ServletRegistration jspServlet = servletContext.getServletRegistration("jsp");
        jspServlet.addMapping(ConfigHelper.getAppJspPath()+"*");
        //注册处理静态资源的默认Servlet
        ServletRegistration defaultServlet = servletContext.getServletRegistration("default");
        defaultServlet.addMapping(ConfigHelper.getAppAssetPath()+"*");

        //初始化框架相关 helper 类
        HelperLoader.init(servletContext);
        System.out.println("初始化完成：");
        System.out.println("controllers:"+ClassHelper.getControllerClassSet().size());
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //获取请求方法与请求路径
        String requestMethod = request.getMethod().toLowerCase();
        String requestPath = request.getPathInfo();

        if(requestPath.equals("/favicon.ico")){
            return;
        }

        //获取 Action 处理器
        Handler handler = ControllerHelper.getHandler(requestMethod,requestPath);
        if(handler != null){
            //获取 Controller  类和 Bean 实例
            Class<?> controllerClass = handler.getControllerClass();
            Object controllerBean = BeanHelper.getBean(controllerClass);

            //创建你请求参数对象
            Param param;
            if(UploadHelper.isMultipart(request)){
                //如果是文件上传
                param = UploadHelper.createParam(request);
            }else{
                //如果不是
                param = RequestHelper.createParam(request);
            }
/*
            Map<String,Object> paramMap = new HashMap<>();
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()){
                String paramName = paramNames.nextElement();
                String paramValue = request.getParameter(paramName);
                paramMap.put(paramName,paramValue);
            }
            String body = CodeUtil.decodeURL(StreamUtil.getString(request.getInputStream()));
            if(StringUtil.isNotEmpty(body)){
                String[] params = body.split("&");
                if(ArrayUtil.isNotEmpty(params)){
                    for (String param:params){
                        String[] ary = param.split("=");
                        if(ArrayUtil.isNotEmpty(ary) && ary.length == 2){
                            String paramName = ary[0];
                            String paramValue = ary[1];
                            paramMap.put(paramName,paramValue);
                        }
                    }
                }
            }
            Param param = new Param(paramMap);*/
            //调用 Action方法
            Method actionMethod = handler.getActionMethod();
            Object result;

            //TODO:这里非常死，开发如果想灵活的写或不写param参数，必须根据前台请求来，否则报错
            //TODO:最终达到效果，param可以获取概览，也可以通过具体参数直接获取
            //TODO:包括文件
            if (param.isEmpty()){
                 result = ReflectionUtil.invokeMethod(controllerBean,actionMethod);
            }else{
                result = ReflectionUtil.invokeMethod(controllerBean,actionMethod,param);
            }
            if(result instanceof View){
                handleViewResult((View)result,request,response);
            } else if (result instanceof Data){
                handleDataResult((Data)result,response);
            }
        }
    }

    private void handleViewResult(View view,HttpServletRequest request,HttpServletResponse response) throws IOException,ServletException{
        //返回JSP页面或者请求跳转
        String path = view.getPath();
        if (StringUtil.isNotEmpty(path)){
            //TODO:什么叫 startWith("/") 这样就认为是浏览器跳转了?
            if(path.startsWith("/")){
                response.sendRedirect(request.getContextPath()+path);
            }else{
                Map<String,Object> model = view.getModel();
                for(Map.Entry<String,Object> entry:model.entrySet()){
                    request.setAttribute(entry.getKey(),entry.getValue());
                }
                request.getRequestDispatcher(ConfigHelper.getAppJspPath()+path).forward(request,response);
            }
        }
    }

    private void handleDataResult(Data data,HttpServletResponse response) throws IOException{
        //返回JSON数据
        Object model = data.getModel();
        if(model != null){
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter writer = response.getWriter();
            String json = JsonUtil.toJson(model);
            writer.write(json);
            writer.flush();
            writer.close();
        }
    }
}
