package uk.co.mackenney.automation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="automation")
public class ServerAutomationConfiguration {

    private int processorCorePoolSize;
    public void setProcessorCorePoolSize(int processorCorePoolSize) {
        this.processorCorePoolSize = processorCorePoolSize;
    }
    public int getProcessorCorePoolSize() {
        return processorCorePoolSize;
    }


    private int processorMaxPoolSize;
    public void setProcessorMaxPoolSize(int processorMaxPoolSize) {
        this.processorMaxPoolSize = processorMaxPoolSize;
    }
    public int getProcessorMaxPoolSize() {
        return processorMaxPoolSize;
    }

}


