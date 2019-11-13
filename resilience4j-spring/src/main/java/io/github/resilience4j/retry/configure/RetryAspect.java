/*
 * Copyright 2019 Mahmoud Romeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.retry.configure;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;

import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.utils.ProceedingJoinPointHelper;

/**
 * This Spring AOP aspect intercepts all methods which are annotated with a
 * {@link Retry} annotation. The aspect will handle methods that return a
 * RxJava2 reactive type, Spring Reactor reactive type, CompletionStage type, or
 * value type.
 *
 * The RetryRegistry is used to retrieve an instance of a Retry for a specific
 * name.
 *
 * Given a method like this:
 * <pre><code>
 *     {@literal @}Retry(name = "myService")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code></pre> each time the {@code #fancyName(String)} method is invoked, the
 * method's execution will pass through a a
 * {@link io.github.resilience4j.retry.Retry} according to the given config.
 *
 * The fallbackMethod parameter signature must match either:
 *
 * 1) The method parameter signature on the annotated method or 2) The method
 * parameter signature with a matching exception type as the last parameter on
 * the annotated method
 */
@Aspect
public class RetryAspect implements Ordered {

    private final RetryAspectHelper retryAspectHelper;
    private final RetryConfigurationProperties retryConfigurationProperties;

    public RetryAspect(RetryAspectHelper retryAspectHelper, RetryConfigurationProperties retryConfigurationProperties) {
        this.retryAspectHelper = retryAspectHelper;
        this.retryConfigurationProperties = retryConfigurationProperties;
    }

    @Pointcut(value = "@within(retry) || @annotation(retry)", argNames = "retry")
    public void matchAnnotatedClassOrMethod(Retry retry) {
    }

    @Around(value = "matchAnnotatedClassOrMethod(retryAnnotation)", argNames = "proceedingJoinPoint, retryAnnotation")
    public Object retryAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, @Nullable Retry retryAnnotation) throws Throwable {
        ProceedingJoinPointHelper joinPointHelper = ProceedingJoinPointHelper.prepareFor(proceedingJoinPoint);
        if (retryAnnotation == null) {
            retryAnnotation = joinPointHelper.getClassAnnotation(Retry.class);
        }
        if (retryAnnotation == null) { //because annotations wasn't found
            return proceedingJoinPoint.proceed();
        }
        retryAspectHelper.decorate(joinPointHelper, retryAnnotation);
        return joinPointHelper.getDecoratedProceedCall().apply();
    }

    @Override
    public int getOrder() {
        return retryConfigurationProperties.getRetryAspectOrder();
    }
}
