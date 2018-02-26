package org.axe.annotation.mvc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于{@link RequestParam}默认值
 * 例子：@Default("paramValue")
 * 
 * 用于{@link RequestEntity}默认值
 * 例子：@Default({"name:val_1","age:val_2"})
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Default {
	
	String[] value() default {};
}