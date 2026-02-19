package com.example.unis_rssol.global.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Admin 권한이 필요한 API에 적용하는 어노테이션
 * rssolewha@gmail.com 이메일을 가진 사용자만 접근 가능
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminOnly {
}

