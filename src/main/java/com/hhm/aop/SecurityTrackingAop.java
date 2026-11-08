package com.hhm.aop;

import com.hhm.service.SecurityEventService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SecurityTrackingAop {

    private final SecurityEventService securityEventService;

    public SecurityTrackingAop(SecurityEventService securityEventService) {
        this.securityEventService = securityEventService;
    }

    @Pointcut("execution(* com.hhm.controller.*.*(..))")
    public void anyControllerMethod() {}

    @Before("anyControllerMethod()")
    public void trackEndpointAccess(JoinPoint jp) {
        String controller = jp.getTarget().getClass().getSimpleName();
        String method     = jp.getSignature().getName();
        securityEventService.recordEndpointAccess(controller, method);
    }
}
