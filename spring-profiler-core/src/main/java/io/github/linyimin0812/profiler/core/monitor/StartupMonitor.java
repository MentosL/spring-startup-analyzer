package io.github.linyimin0812.profiler.core.monitor;

import io.github.linyimin0812.profiler.common.logger.LogFactory;
import io.github.linyimin0812.profiler.common.logger.Logger;
import io.github.linyimin0812.profiler.core.container.IocContainer;
import io.github.linyimin0812.profiler.core.monitor.check.AppStatus;
import io.github.linyimin0812.profiler.core.monitor.check.AppStatusCheckService;
import io.github.linyimin0812.profiler.api.Lifecycle;
import org.kohsuke.MetaInfServices;

/**
 * 应用启动检测
 * @author linyimin
 **/
@MetaInfServices
public class StartupMonitor implements Lifecycle {

    private final Logger logger = LogFactory.getStartupLogger();

    private void checkStatus() {

        int count = 0;

        while (true) {
            boolean isRunning = IocContainer.getComponents(AppStatusCheckService.class).stream().anyMatch(service -> service.check() == AppStatus.running);
            if (isRunning) {
                break;
            }

            if (count++ % 10 == 0) {
                logger.info(StartupMonitor.class, "app initializing {} s", count);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error(StartupMonitor.class, "sleep interrupt", e);
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 应用启动结束后容器终止
        IocContainer.stop();
    }

    @Override
    public void start() {
        logger.info(StartupMonitor.class, "==========StartupMonitor start========");
        Thread startupMonitorThread = new Thread(this::checkStatus);
        startupMonitorThread.setName("StartupMonitor-Thread");
        startupMonitorThread.start();
    }

    @Override
    public void stop() {
        // 应用启动结束后容器终止
        logger.info(StartupMonitor.class, "==========StartupMonitor stop========");
    }
}
