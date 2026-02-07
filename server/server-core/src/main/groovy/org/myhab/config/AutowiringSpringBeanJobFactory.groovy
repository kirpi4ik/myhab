package org.myhab.config

import groovy.util.logging.Slf4j
import org.quartz.spi.TriggerFiredBundle
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.scheduling.quartz.SpringBeanJobFactory

/**
 * Custom JobFactory that enables Spring autowiring for Quartz jobs.
 * This ensures that jobs created by Quartz have their @Autowired dependencies injected.
 */
@Slf4j
class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {
    
    private transient AutowireCapableBeanFactory beanFactory
    
    @Override
    void setApplicationContext(ApplicationContext context) {
        this.beanFactory = context.getAutowireCapableBeanFactory()
        log.info("AutowiringSpringBeanJobFactory initialized with ApplicationContext")
    }
    
    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
        Object job = super.createJobInstance(bundle)
        
        log.info("Creating job instance: ${job.class.simpleName}, beanFactory=${beanFactory != null}")
        
        if (beanFactory != null) {
            // Autowire by type and initialize the bean
            beanFactory.autowireBeanProperties(job, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false)
            beanFactory.initializeBean(job, job.class.name)
            log.info("Autowired and initialized job instance: ${job.class.simpleName}")
        } else {
            log.error("BeanFactory is null - cannot autowire job: ${job.class.simpleName}")
        }
        
        return job
    }
}

