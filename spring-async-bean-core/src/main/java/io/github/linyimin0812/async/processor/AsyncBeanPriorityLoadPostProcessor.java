package io.github.linyimin0812.async.processor;

import io.github.linyimin0812.async.config.AsyncConfig;
import io.github.linyimin0812.profiler.common.logger.LogFactory;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.List;


/**
 * @author yiminlin
 * @date 2023/05/14 15:32
 **/
public class AsyncBeanPriorityLoadPostProcessor extends InstantiationAwareBeanPostProcessorAdapter implements BeanFactoryAware {

    private final Logger logger = LogFactory.getStartupLogger();

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

        if (!AsyncConfig.getInstance().getAsyncBeanProperties().isAsyncBeanPriorityLoadEnable()) {
            return;
        }

        List<String> asyncBeans = AsyncConfig.getInstance().getAsyncBeanProperties().getBeanNames();

        for (String beanName : asyncBeans) {

            if (beanFactory instanceof DefaultListableBeanFactory && !((DefaultListableBeanFactory) beanFactory).containsBeanDefinition(beanName)) {
                logger.warn("BeanDefinition of bean {} is not exist.", beanName);
                continue;
            }

            logger.info("async init bean: {}", beanName);
            beanFactory.getBean(beanName);
        }
    }
}
