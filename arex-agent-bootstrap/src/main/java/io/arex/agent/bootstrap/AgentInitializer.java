package io.arex.agent.bootstrap;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class AgentInitializer {

    private static ClassLoader agentClassLoader;

    public static void initialize(Instrumentation inst, File agentFile, String agentArgs)
            throws Exception {
        if (agentClassLoader != null) {
            return;
        }

        System.setProperty("arex-agent-jar-file-path", agentFile.getAbsolutePath());

        agentClassLoader = createAgentClassLoader(agentFile);
        InstrumentationHolder.setAgentClassLoader(agentClassLoader);
        AgentInstaller installer = createAgentInstaller(inst, agentFile, agentArgs);
        installer.install();
    }

    private static AgentClassLoader createAgentClassLoader(File agentFile) throws Exception {
        return new AgentClassLoader(agentFile, getParentClassLoader(), null);
    }

    private static AgentInstaller createAgentInstaller(Instrumentation inst, File file, String agentArgs) throws Exception {
        Class<?> clazz =
                agentClassLoader.loadClass("io.arex.agent.instrumentation.InstrumentationInstaller");
        Constructor<?> constructor = clazz.getDeclaredConstructor(Instrumentation.class, File.class, String.class);
        return (AgentInstaller) constructor.newInstance(inst, file, agentArgs);
    }

    private static ClassLoader getParentClassLoader() throws Exception {
        ClassLoader parent;
        if (System.getProperty("java.version").startsWith("1.")) {
            // java8
            parent = null;
        } else {
            // java9
            Method method = ClassLoader.class.getDeclaredMethod("getPlatformClassLoader");
            parent = (ClassLoader) method.invoke(null);
        }

        return parent;
    }
}
