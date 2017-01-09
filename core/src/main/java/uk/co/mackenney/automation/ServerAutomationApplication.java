package uk.co.mackenney.automation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties
public class ServerAutomationApplication {
    private static Logger log = LoggerFactory.getLogger(ServerAutomationApplication.class);

    @Autowired
    private ServerAutomationConfiguration config;

    public static void main(String[] args) {
        SpringApplication.run(ServerAutomationApplication.class, args);
    }

    @Bean
    public Executor processorExecutor() {
        log.info("Initializing processor pool.  CorePoolSize: " + config.getProcessorCorePoolSize() + "  MaxPoolSize: " + config.getProcessorMaxPoolSize());
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(config.getProcessorCorePoolSize());
        pool.setMaxPoolSize(config.getProcessorMaxPoolSize());
        pool.setThreadNamePrefix("processor-");
        pool.setWaitForTasksToCompleteOnShutdown(true);
        return pool;
    }

} 
