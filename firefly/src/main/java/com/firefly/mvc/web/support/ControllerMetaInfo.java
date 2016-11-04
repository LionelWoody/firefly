package com.firefly.mvc.web.support;

import com.firefly.annotation.RequestMapping;

import javax.servlet.MultipartConfigElement;
import javax.servlet.annotation.MultipartConfig;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ControllerMetaInfo extends HandlerMetaInfo {

    private final Set<String> allowHttpMethod;
    private final MultipartConfigElement multipartConfigElement;

    public ControllerMetaInfo(Object object, Method method) {
        super(object, method);
        allowHttpMethod = new HashSet<String>(Arrays.asList(method.getAnnotation(RequestMapping.class).method()));
        MultipartConfig multipartConfig = object.getClass().getAnnotation(MultipartConfig.class);
        if (multipartConfig != null) {
            multipartConfigElement = new MultipartConfigElement(multipartConfig);
        } else {
            multipartConfigElement = null;
        }
    }

    public MultipartConfigElement getMultipartConfigElement() {
        return multipartConfigElement;
    }

    public boolean allowMethod(String method) {
        return allowHttpMethod.contains(method);
    }

    public String getAllowMethod() {
        StringBuilder s = new StringBuilder();
        for (String m : allowHttpMethod) {
            s.append(m).append(',');
        }
        s.deleteCharAt(s.length() - 1);
        return s.toString();
    }

    @Override
    public String toString() {
        return "ControllerMetaInfo [allowHttpMethod=" + allowHttpMethod + "]";
    }

}
