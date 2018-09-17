package com.efun.efevent.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事件关联注解
 * 
 * @author Ken
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventMapping {
	
	/**
	 * 事件标识
	 * 
	 * @return
	 */
	public String value() default "";

}
