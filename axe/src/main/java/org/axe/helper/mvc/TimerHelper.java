/**
 * MIT License
 * 
 * Copyright (c) 2017 CaiDongyu
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *//*
package org.axe.helper.mvc;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.axe.extra.abc_thread.SerialExecutor;
import org.axe.extra.abc_thread.TaskPack;
import org.axe.extra.abc_thread.TaskPackBusController;
import org.axe.helper.ioc.BeanHelper;
import org.axe.helper.ioc.ClassHelper;
import org.axe.interface_.base.Helper;
import org.axe.interface_.mvc.Timer;
import org.axe.util.CollectionUtil;
import org.axe.util.LogUtil;
import org.axe.util.ReflectionUtil;

*//**
 * Timer 助手 ，类似BeanHelper管理加载所有bean一样，这里是托管Timer
 * 之所以和BeanHelper分开，因为BeanHelper托管了Controller和Service Timer不应该放一起 Created by
 * CaiDongYu on 2020/4/7.
 *//*
public final class TimerHelper implements Helper {
	private static final Map<String,Timer> TIMER_MAP = new HashMap<>();
	
	@Override
	public void init() throws Exception{
		synchronized (this) {
			Set<Class<?>> timerClassSet = ClassHelper.getClassSetBySuper(Timer.class);
				if (CollectionUtil.isNotEmpty(timerClassSet)) {
				for (Class<?> timerClass : timerClassSet) {
					boolean isAbstract = Modifier.isAbstract(timerClass.getModifiers());
					if(isAbstract) continue;
					
					Timer timer = ReflectionUtil.newInstance(timerClass);
					
					if(TIMER_MAP.containsKey(timer.name())){
						throw new Exception("find the same timer name:"+timer.name()+" class:"+timer.getClass()+" === "+TIMER_MAP.get(timer.name()).getClass());
					}
					TIMER_MAP.put(timer.name(), timer);
					BeanHelper.setBean(timerClass, timer);
				}
			}
		}
	}

	public static Map<String,Timer> getTimers() {
		return TIMER_MAP;
	}

	@Override
	public void onStartUp() throws Exception {
		if(CollectionUtil.isNotEmpty(TIMER_MAP)){
			//Timer的启动
			new Thread("Timer-Watcher-Thread"){
				private final Set<String> TIMER_IS_RUNNING = new HashSet<>();
				
				public void run() {
					TaskPackBusController tpBusController = BeanHelper.getBean(TaskPackBusController.class);
					tpBusController.start();
					while(true){
						for(final Timer timer:TIMER_MAP.values()){
							try {
								if(!timer.canExecuteNow()){
									continue;
								}else{
									if(TIMER_IS_RUNNING.contains(timer.name())){
										continue;
									}
								}
								
								TIMER_IS_RUNNING.add(timer.name());
								TaskPack tp = new TaskPack(timer.name()) {
									@Override
									public boolean task(SerialExecutor executor) {
										try {
											timer.doSomething();
											return false;
										} catch (Exception e) {
											LogUtil.error(e);
											return false;
										}finally{
											TIMER_IS_RUNNING.remove(timer.name());
										}
									}
								};
								tpBusController.addTaskPack(tp);
							} catch (Exception e) {
								LogUtil.error(e);
							}
						}
						try {
							Thread.sleep(100);//100毫秒钟是定时器的最小周期
						} catch (InterruptedException e) {}
					}
				};
			}.start();
		}
	}

}
*/