package io.github.linyimin0812.profiler.core.container;

import ch.qos.logback.classic.Logger;
import io.github.linyimin0812.profiler.common.logger.LogFactory;
import io.github.linyimin0812.profiler.common.markdown.MarkdownWriter;
import io.github.linyimin0812.profiler.common.settings.ProfilerSettings;
import io.github.linyimin0812.profiler.core.http.SimpleHttpServer;
import io.github.linyimin0812.profiler.core.monitor.StartupMonitor;
import io.github.linyimin0812.profiler.api.EventListener;
import io.github.linyimin0812.profiler.api.Lifecycle;
import io.github.linyimin0812.profiler.core.monitor.check.AppStatusCheckService;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoBuilder;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author linyimin
 * @date 2023/04/13 11:45
 * @description 生命周期实例容器，由容器统一控制
 **/
public class IocContainer {


    private static final Logger logger = LogFactory.getStartupLogger();

    private static final MutablePicoContainer container = new PicoBuilder().withSetterInjection().withCaching().withLifecycle().build();

    private static final AtomicBoolean started = new AtomicBoolean();

    private static final AtomicBoolean stopped = new AtomicBoolean();

    /**
     * 启动服务容器
     */
    public static void start() {
        if (started.compareAndSet(false, true)) {

            container.start();

            for (Lifecycle lifecycle : ServiceLoader.load(Lifecycle.class, IocContainer.class.getClassLoader())) {
                container.addComponent(lifecycle);
            }

            for (EventListener listener : ServiceLoader.load(EventListener.class, IocContainer.class.getClassLoader())) {
                container.addComponent(listener);
            }

            for (AppStatusCheckService service : ServiceLoader.load(AppStatusCheckService.class, StartupMonitor.class.getClassLoader())) {
                service.init();
                container.addComponent(service);
            }

            SimpleHttpServer.start();
        }
    }

    /**
     * 停止容器
     */
    public static void stop() {
        if (stopped.compareAndSet(false, true)) {
            container.dispose();
            MarkdownWriter.upload();
        }

        String endpoint = ProfilerSettings.getProperty("java-profiler.jaeger.ui.endpoint");
        String prompt = String.format("======= java-profiler-boost stop, click %s to view detailed info about the startup process ======", endpoint);

        logger.info(prompt);
        System.out.println(prompt);

    }

    public static <T> T getComponent(Class<T> componentType) {
        return container.getComponent(componentType);
    }

    public static <T> List<T> getComponents(Class<T> componentType) {
        return container.getComponents(componentType);
    }
}