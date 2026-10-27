package com.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAop {

    // ── Pointcuts ──────────────────────────────────────────────────────────────

    @Pointcut("execution(* com.controller.*.*(..))")
    public void controllerAspect() {}

    @Pointcut("execution(* com.service.*.*(..))")
    public void serviceAspect() {}

    // ── Controller advice ──────────────────────────────────────────────────────

    /** Fires before the controller method executes — logs method name and arguments. */
    @Before("controllerAspect()")
    public void logBeforeController(JoinPoint joinPoint) {
        log.info("[CTRL] --> {} | args: {}",
                joinPoint.getSignature().getName(),
                Arrays.toString(joinPoint.getArgs()));
    }

    /** Fires after the controller method exits — whether it succeeded or threw. */
    @After("controllerAspect()")
    public void logAfterController(JoinPoint joinPoint) {
        log.info("[CTRL] <-- {} | exited (success or error)",
                joinPoint.getSignature().getName());
    }

    /** Fires only on successful return — logs the return value. */
    @AfterReturning(pointcut = "controllerAspect()", returning = "result")
    public void logAfterReturningController(JoinPoint joinPoint, Object result) {
        log.info("[CTRL] OK  {} | returned: {}",
                joinPoint.getSignature().getName(), result);
    }

    /** Fires only when an exception escapes the controller — logs the error. */
    @AfterThrowing(pointcut = "controllerAspect()", throwing = "exception")
    public void logAfterThrowingController(JoinPoint joinPoint, Throwable exception) {
        log.error("[CTRL] ERR {} | threw: {}",
                joinPoint.getSignature().getName(), exception.getMessage());
    }

    /**
     * Wraps the entire controller method execution to measure elapsed time.
     * @Around goes first; the other advices fire inside pjp.proceed().
     * Execution order: @Around start → @Before → method → @AfterReturning/@AfterThrowing
     *                  → @After → @Around end.
     */
    @Around("controllerAspect()")
    public Object timeController(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("[CTRL] TIME {} completed in {} ms",
                    pjp.getSignature().getName(), System.currentTimeMillis() - start);
            return result;
        } catch (Throwable t) {
            log.error("[CTRL] TIME {} failed after {} ms",
                    pjp.getSignature().getName(), System.currentTimeMillis() - start);
            throw t;
        }
    }

    // ── Service advice ─────────────────────────────────────────────────────────

    /** Logs the service method and its arguments before execution. */
    @Before("serviceAspect()")
    public void logBeforeService(JoinPoint joinPoint) {
        log.debug("[SVC]  --> {}.{} | args: {}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                Arrays.toString(joinPoint.getArgs()));
    }

    /** Logs the service return value on success. */
    @AfterReturning(pointcut = "serviceAspect()", returning = "result")
    public void logAfterReturningService(JoinPoint joinPoint, Object result) {
        log.debug("[SVC]  OK  {} | returned: {}",
                joinPoint.getSignature().getName(), result);
    }

    /** Logs business-rule violations thrown by the service layer. */
    @AfterThrowing(pointcut = "serviceAspect()", throwing = "exception")
    public void logAfterThrowingService(JoinPoint joinPoint, Throwable exception) {
        log.warn("[SVC]  ERR {} | threw: {}",
                joinPoint.getSignature().getName(), exception.getMessage());
    }
}
