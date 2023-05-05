package com.github.linyimin.profiler.core.enhance;

import ch.qos.logback.classic.Logger;
import com.alibaba.bytekit.asm.MethodProcessor;
import com.alibaba.bytekit.asm.interceptor.InterceptorProcessor;
import com.alibaba.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.deps.org.objectweb.asm.ClassReader;
import com.alibaba.deps.org.objectweb.asm.Opcodes;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.github.linyimin.Bridge;
import com.github.linyimin.profiler.common.instruction.InstrumentationHolder;
import com.github.linyimin.profiler.common.logger.LogFactory;
import com.github.linyimin.profiler.core.container.IocContainer;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 此类由agent加载，入口为构造函数
 * @author linyimin
 * @date 2023/04/17 17:43
 **/
public class ProfilerClassFileTransformer implements ClassFileTransformer {

    private final Logger logger = LogFactory.getTransFormLogger();

    private final Object DUMMY = new Object();
    private final Map<String, Object> enhancedObject = new ConcurrentHashMap<>();

    public ProfilerClassFileTransformer(Instrumentation instrumentation, String args) {

        Bridge.setBridge(new EventDispatcher());
        InstrumentationHolder.setInstrumentation(instrumentation);

        IocContainer.start();

    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        if (className == null) {
            return null;
        }

        // 排除系统类及spring中的动态代理类
        if (className.contains("sun/") || className.contains("java/") || className.contains("javax/") || className.contains("CGLIB")) {
            return null;
        }

        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        ClassReader classReader = AsmUtils.toClassNode(classfileBuffer, classNode);
        classNode = AsmUtils.removeJSRInstructions(classNode);

        // 把自己排除掉
        if (Matcher.isJavaProfilerFamily(classNode)) {
            return null;
        }

        if (!Matcher.isMatchClass(classNode)) {
            return null;
        }

        List<MethodNode> methodNodes = classNode.methods;

        // 生成增强字节码
        DefaultInterceptorClassParser defaultInterceptorClassParser = new DefaultInterceptorClassParser();

        List<InterceptorProcessor> interceptorProcessors = new ArrayList<>(defaultInterceptorClassParser.parse(Interceptor.class));

        for (MethodNode methodNode : methodNodes) {

            if (isEnhanceBefore(loader, classNode, methodNode)) {
                continue;
            }

            if (!Matcher.isMatchMethod(classNode, methodNode)) {
                continue;
            }

            MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode);
            for (InterceptorProcessor interceptor : interceptorProcessors) {
                try {
                    interceptor.process(methodProcessor);
                    logger.info("transform: {}", getCacheKey(loader, classNode, methodNode));
                } catch (Throwable e) {
                    logger.error("enhancer error, class: {}, method: {}, interceptor: {}, error: {}",
                            classNode.name, methodNode.name, interceptor.getClass().getName(), e);
                }
            }

            cacheEnhanceObject(loader, classNode, methodNode);

        }

        return AsmUtils.toBytes(classNode, loader, classReader);

    }

    private void cacheEnhanceObject(ClassLoader loader, ClassNode classNode, MethodNode methodNode) {
        enhancedObject.put(getCacheKey(loader, classNode, methodNode), DUMMY);
    }

    private boolean isEnhanceBefore(ClassLoader loader, ClassNode classNode, MethodNode methodNode) {
        return enhancedObject.containsKey(getCacheKey(loader, classNode, methodNode));
    }

    private String getCacheKey(ClassLoader loader, ClassNode classNode, MethodNode methodNode) {

        String loaderName = loader.getClass().getName();
        String className = classNode.name;
        String methodName = methodNode.name;

        Type methodType = Type.getMethodType(methodNode.desc);
        StringBuilder argTypes = new StringBuilder();

        for (Type type : methodType.getArgumentTypes()) {
            argTypes.append(type.getClassName());
        }

        return loaderName + "#" + className + "#" + methodName + "#" + argTypes;
    }

}
