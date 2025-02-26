package io.arex.agent.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * simple class loader
 * todo: add plugin jar support
 */
public class AgentClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private JarInfo agentJarInfo;
    private JarFile agentJarFile;
    private List<JarInfo> extensionJarFiles;

    public AgentClassLoader(File jarFile, ClassLoader parent, String[] extensionJars) {
        super(new URL[]{}, parent);

        try {
            this.agentJarFile = new JarFile(jarFile, false);
            agentJarInfo = new JarInfo(agentJarFile, jarFile);
            extensionJarFiles = getExtensionJarFiles(jarFile);
            for (JarInfo jarInfo : extensionJarFiles) {
                super.addURL(jarInfo.getSourceFile().toURI().toURL());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open agent jar", e);
        }
    }

    private List<JarInfo> getExtensionJarFiles(File jarFile) {
        String extensionDir = jarFile.getParent() + "\\extensions\\";
        List<JarInfo> extensionJarFiles = new ArrayList<>();
        File[] extensionFiles = new File(extensionDir).listFiles(this::isJar);

        if (extensionFiles == null) {
            return extensionJarFiles;
        }

        for (File file : extensionFiles) {
            if (!file.getAbsolutePath().equals(jarFile.getAbsolutePath())) {
                try {
                    JarInfo jarInfo = new JarInfo(new JarFile(file, false), file);
                    extensionJarFiles.add(jarInfo);
                } catch (IOException e) {
                    System.err.printf(
                        String.format("Add extension file failed, file: %s%n", file.getAbsolutePath()));
                }
            }
        }

        return extensionJarFiles;
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            if (clazz == null) {
                clazz = findClass(name);
            }

            if (clazz == null) {
                clazz = super.loadClass(name, false);
            }
            if (resolve) {
                resolveClass(clazz);
            }

            return clazz;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        JarEntryInfo jarEntryInfo = findJarEntry(name.replace('.', '/') + ".class");
        if (jarEntryInfo != null && jarEntryInfo.getJarEntry() != null) {
            byte[] bytes;
            try {
                bytes = getJarEntryBytes(jarEntryInfo);
            } catch (IOException exception) {
                throw new ClassNotFoundException(name, exception);
            }

            definePackageIfNeeded(jarEntryInfo, name);
            return defineClass(name, bytes);
        }
        return null;
    }

    public Class<?> defineClass(String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }

    private void definePackageIfNeeded(JarEntryInfo jarEntryInfo, String className) {
        String packageName = getPackageName(className);
        if (packageName == null) {
            return;
        }
        if (getPackage(packageName) == null) {
            try {
                definePackage(packageName, jarEntryInfo.getJarInfo().getJarFile().getManifest(),
                    jarEntryInfo.getJarInfo().getSourceFile().toURI().toURL());
            } catch (Exception exception) {
                if (getPackage(packageName) == null) {
                    throw new IllegalStateException("Failed to define package", exception);
                }
            }
        }
    }

    private static String getPackageName(String className) {
        int index = className.lastIndexOf('.');
        return index == -1 ? null : className.substring(0, index);
    }

    private byte[] getJarEntryBytes(JarEntryInfo jarEntryInfo) throws IOException {
        int size = (int) jarEntryInfo.getJarEntry().getSize();
        byte[] buffer = new byte[size];
        try (InputStream is = jarEntryInfo.getJarInfo().getJarFile().getInputStream(jarEntryInfo.getJarEntry())) {
            int offset = 0;
            int read;

            while (offset < size && (read = is.read(buffer, offset, size - offset)) != -1) {
                offset += read;
            }
        }

        return buffer;
    }

    // need to cache
    private JarEntryInfo findJarEntry(String name) {
        JarEntry jarEntry = agentJarInfo.getJarFile().getJarEntry(name);
        if (jarEntry != null) {
            return new JarEntryInfo(name, jarEntry, agentJarInfo);
        }

        for (JarInfo jarInfo : extensionJarFiles) {
            jarEntry = jarInfo.getJarFile().getJarEntry(name);
            if (jarEntry != null) {
                return new JarEntryInfo(name, jarEntry, jarInfo);
            }
        }

        return null;
    }

    private URL getJarEntryUrl(JarEntryInfo jarInfo) {
        if (jarInfo != null && jarInfo.getJarEntry() != null) {
            try {
                return new URL(
                    "jar:file:" + jarInfo.getJarInfo().sourceFile.getAbsolutePath() + "!/" + jarInfo.getJarEntry()
                        .getName());
            } catch (MalformedURLException e) {
                throw new IllegalStateException(jarInfo.getJarEntry().getName(), e);
            }
        }
        return null;
    }

    @Override
    public URL findResource(String name) {
        URL url = getJarEntryUrl(findJarEntry(name));
        if (url != null) {
            return url;
        }
        return super.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> superResource = super.findResources(name);
        URL url = getJarEntryUrl(findJarEntry(name));
        if (url == null) {
            return superResource;
        }

        List<URL> resources = new LinkedList<>();
        resources.add(url);
        while (superResource.hasMoreElements()) {
            resources.add(superResource.nextElement());
        }
        final Iterator<URL> iterator = resources.iterator();
        return new Enumeration<URL>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public URL nextElement() {
                return iterator.next();
            }
        };
    }

    private boolean isJar(File f) {
        return f.isFile() && f.getName().endsWith(".jar");
    }

    private static class JarEntryInfo {

        private final String className;
        private final JarEntry jarEntry;
        private final JarInfo jarInfo;

        private JarEntryInfo(String className, JarEntry jarEntry, JarInfo jarInfo) {
            this.className = className;
            this.jarEntry = jarEntry;
            this.jarInfo = jarInfo;
        }

        public JarEntry getJarEntry() {
            return jarEntry;
        }

        public JarInfo getJarInfo() {
            return jarInfo;
        }
    }

    private static class JarInfo {

        private final JarFile jarFile;
        private final File sourceFile;

        private JarInfo(JarFile jarFile, File sourceFile) {
            this.jarFile = jarFile;
            this.sourceFile = sourceFile;
        }

        public JarFile getJarFile() {
            return jarFile;
        }

        public File getSourceFile() {
            return sourceFile;
        }
    }
}
