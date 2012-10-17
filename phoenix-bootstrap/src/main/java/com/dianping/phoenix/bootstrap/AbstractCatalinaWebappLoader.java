package com.dianping.phoenix.bootstrap;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarFile;

import javax.servlet.ServletContext;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappClassLoader;
import org.apache.catalina.loader.WebappLoader;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import com.dianping.phoenix.spi.ClasspathBuilder;
import com.dianping.phoenix.spi.WebappProvider;
import com.dianping.phoenix.spi.internal.DefaultClasspathBuilder;
import com.dianping.phoenix.spi.internal.StandardWebappProvider;

public abstract class AbstractCatalinaWebappLoader extends WebappLoader {
	private static Log m_log = LogFactory.getLog(AbstractCatalinaWebappLoader.class);

	private WebappProvider m_appProvider;

	private WebappProvider m_kernelProvider;

	private ClassLoader m_webappClassloader;

	private String m_kernelDocBase;

	private boolean m_debug = true;

	public AbstractCatalinaWebappLoader() {
	}

	public AbstractCatalinaWebappLoader(ClassLoader classloader) {
		super(classloader);
	}

	WebappClassLoader adjustWebappClassloader(WebappClassLoader classloader) {
		try {
			clearLoadedJars(classloader);

			ClasspathBuilder builder = new DefaultClasspathBuilder();
			List<URL> urls = builder.build(m_kernelProvider, m_appProvider);

			for (URL url : urls) {
				super.addRepository(url.toExternalForm());
			}

			if (m_debug) {
				m_log.info(String.format("Webapp classpath: %s.", Arrays.asList(classloader.getURLs())));
			}
			return classloader;
		} catch (Exception e) {
			throw new RuntimeException("Error when adjusting webapp classloader!", e);
		}
	}

	void clearLoadedJars(WebappClassLoader classloader) throws Exception {
		List<String> loaderRepositories = getFieldValue(this, WebappLoader.class, "loaderRepositories");
		URL[] repositoryURLs = getFieldValue(classloader, "repositoryURLs");
		// JarFile[] jarFiles = getFieldValue(classloader, "jarFiles");
		// String[] jarNames = getFieldValue(classloader, "jarNames");
		// File[] jarRealFiles = getFieldValue(classloader, "jarRealFiles");
		// String[] paths = getFieldValue(classloader, "paths");

		for (int i = loaderRepositories.size() - 1; i >= 0; i--) {
			String repository = loaderRepositories.get(i);

			if (repository.endsWith(".jar")) {
				loaderRepositories.remove(i);
			}
		}

		List<URL> urls = new ArrayList<URL>();

		for (URL url : repositoryURLs) {
			if (!url.toExternalForm().endsWith(".jar")) {
				urls.add(url);
			}
		}

		setFieldValue(classloader, "repositoryURLs", urls.toArray(new URL[0]));
		setFieldValue(classloader, "jarFiles", new JarFile[0]);
		setFieldValue(classloader, "jarNames", new String[0]);
		setFieldValue(classloader, "jarRealFiles", new File[0]);
		setFieldValue(classloader, "paths", new String[0]);
	}

	ClassLoader createBootstrapClassloader() {
		try {
			List<URL> urls = new ArrayList<URL>();

			for (File entry : m_kernelProvider.getClasspathEntries()) {
				File file = entry.getCanonicalFile();

				if (file.isDirectory() || file.getPath().endsWith(".jar")) {
					urls.add(file.toURI().toURL());
				}
			}

			if (m_debug) {
				m_log.info(String.format("Bootstrap classpath: %s.", urls));
			}

			ClassLoader classloader = getClass().getClassLoader();

			if (classloader instanceof URLClassLoader) {
				Object ucp = getFieldValue(classloader, classloader.getClass().getSuperclass(), "ucp");
				List<URL> pathes = getFieldValue(ucp, ucp.getClass(), "path");
				int len = pathes.size();

				for (int i = len - 1; i >= 0; i--) {
					URL path = pathes.get(i);

					if (shouldIgnoredByBootstrapClassloader(path)) {
						pathes.remove(i);

						if (m_debug) {
							m_log.info("Entry " + path + " ignored!");
						}
					}
				}
			}

			return new URLClassLoader(urls.toArray(new URL[0]), classloader);
		} catch (Exception e) {
			throw new RuntimeException("Unable to create bootstrap classloader!", e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getFieldValue(Object instance, Class<?> clazz, String fieldName) throws Exception {
		Field field = clazz.getDeclaredField(fieldName);

		if (!field.isAccessible()) {
			field.setAccessible(true);
		}

		return (T) field.get(instance);
	}

	public <T> T getFieldValue(Object instance, String fieldName) throws Exception {
		return getFieldValue(instance, instance.getClass(), fieldName);
	}

	public File getKernelWarRoot() {
		return m_kernelProvider.getWarRoot();
	}

	protected Log getLog() {
		return m_log;
	}

	public ServletContext getServletContext() {
		Container container = super.getContainer();

		if (container instanceof Context) {
			ServletContext servletContext = ((Context) container).getServletContext();

			return servletContext;
		} else {
			throw new RuntimeException("No ServletContext was found!");
		}
	}

	public File getWarRoot() {
		return m_appProvider.getWarRoot();
	}

	/**
	 * The webapp class loader should be used to load all classes for the runtime
	 * request.
	 * 
	 * @return webapp class loader
	 */
	public WebappClassLoader getWebappClassLoader() {
		if (m_webappClassloader != null) {
			return (WebappClassLoader) m_webappClassloader;
		} else {
			throw new IllegalStateException("WebappClassLoader is not ready at this time!");
		}
	}

	protected <T> T loadListener(Class<T> listenerClass, ClassLoader classloader) {
		ServiceLoader<T> serviceLoader = ServiceLoader.load(listenerClass, classloader);

		for (T e : serviceLoader) {
			return e;
		}

		throw new UnsupportedOperationException("No implementation class found in phoenix-kernel war for "
		      + listenerClass);
	}

	protected void prepareWebappProviders(StandardContext ctx) {
		try {
			if (m_kernelProvider == null) {
				m_kernelProvider = new StandardWebappProvider(m_kernelDocBase);
			}

			if (m_appProvider == null) {
				String appDocBase = ctx.getDocBase();

				m_appProvider = new StandardWebappProvider(appDocBase);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * For development only!
	 * 
	 * @param appProvider
	 */
	public void setApplicationWebappProvider(WebappProvider appProvider) {
		m_appProvider = appProvider;
	}

	public void setDebug(String debug) {
		m_debug = "true".equals(debug);
	}

	public void setFieldValue(Object instance, Class<?> clazz, String fieldName, Object value) throws Exception {
		Field field = clazz.getDeclaredField(fieldName);

		if (!field.isAccessible()) {
			field.setAccessible(true);
		}

		field.set(instance, value);
	}

	public void setFieldValue(Object instance, String fieldName, Object value) throws Exception {
		setFieldValue(instance, instance.getClass(), fieldName, value);
	}

	/**
	 * For production only!
	 * 
	 * @param kernelDocBase
	 */
	public void setKernelDocBase(String kernelDocBase) {
		m_kernelDocBase = kernelDocBase;
	}

	/**
	 * For development only!
	 * 
	 * @param kernelProvider
	 */
	public void setKernelWebappProvider(WebappProvider kernelProvider) {
		m_kernelProvider = kernelProvider;
	}

	protected abstract boolean shouldIgnoredByBootstrapClassloader(URL url);

	@Override
	public void start() throws LifecycleException {
		super.start();

		m_webappClassloader = adjustWebappClassloader((WebappClassLoader) getClassLoader());
	}

	protected static class Delegate<T extends AbstractCatalinaWebappLoader, S extends LifecycleHandler<T>> implements
	      LifecycleListener {
		private T m_loader;

		private LifecycleHandler<T> m_listener;

		public Delegate(T loader, S listener) {
			m_loader = loader;
			m_listener = listener;
		}

		@Override
		public void lifecycleEvent(LifecycleEvent event) {
			String type = event.getType();

			try {
				if (Lifecycle.INIT_EVENT.equals(type)) {
					m_listener.initializing(m_loader);
				} else if (Lifecycle.BEFORE_START_EVENT.equals(type)) {
					m_listener.beforeStarting(m_loader);
				} else if (Lifecycle.START_EVENT.equals(type)) {
					m_listener.starting(m_loader);
				} else if (Lifecycle.AFTER_START_EVENT.equals(type)) {
					m_listener.afterStarted(m_loader);
				} else if (Lifecycle.STOP_EVENT.equals(type)) {
					m_listener.stopping(m_loader);
				} else if (Lifecycle.DESTROY_EVENT.equals(type)) {
					m_listener.destroying(m_loader);
				} else {
					// ignore it
				}
			} catch (Throwable e) {
				m_loader.getLog().error(String.format("Error when dispatching " + //
				      "lifecycle event(%s) to listener(%s)!", type, m_listener.getClass().getName()), e);
			}
		}
	}

	protected static interface LifecycleHandler<T extends AbstractCatalinaWebappLoader> {
		public void afterStarted(T loader);

		public void beforeStarting(T loader);

		public void destroying(T loader);

		public void initializing(T loader);

		public void starting(T loader);

		public void stopping(T loader);
	}
}
