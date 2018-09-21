package com.efun.efevent.event;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 事件处理器扫描器
 * 
 * @author Ken
 *
 */
public class EventHandlerScanner {

	private static final String PACKAGE_PATH = "com.efun.efevent.event.handle";

	private static List<String> classNames = new ArrayList<String>();

	private static List<Class> classes = new ArrayList<Class>();

	/**
	 * 初始化事件处理器类集合
	 * 
	 * @throws IOException
	 */
	private void initEventHandlerClassNames() throws IOException {
		Enumeration<URL> urls = EventHandlerScanner.class.getClassLoader().getResources(PACKAGE_PATH.replace(".", "/"));
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			String protocol = url.getProtocol();
			if ("file".equals(protocol)) {// 如果是以文件的形式保存在服务器上
				String filePath = URLDecoder.decode(url.getFile(), "UTF-8");// 获取包的物理路径
				parseClassNameFromFile(filePath, PACKAGE_PATH);
			} else if ("jar".equals(protocol)) {// 如果是jar包文件
				JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
				parseClassNameFromJar(PACKAGE_PATH, jar);
			}
		}
	}

	/**
	 * 解析文件名从jar包
	 * 
	 * @param packagePath
	 * @param jar
	 */
	private void parseClassNameFromJar(String packagePath, JarFile jar) {
		String packageDir = packagePath.replace(".", "/");
		Enumeration<JarEntry> entry = jar.entries();
		JarEntry jarEntry;
		String name, className;
		while (entry.hasMoreElements()) {
			jarEntry = entry.nextElement();
			name = jarEntry.getName();
			if (name.charAt(0) == '/') {
				name = name.substring(1);
			}
			if (jarEntry.isDirectory() || !name.startsWith(packageDir) || !name.endsWith(".class")) {
				// 非指定包路径， 非class文件
				continue;
			}

			// 去掉后面的".class", 将路径转为package格式
			className = name.substring(0, name.length() - 6);
			classNames.add(className.replace("/", "."));
		}
	}

	/**
	 * 获取所有事件处理器的实现类集合
	 * 
	 * @return
	 * @throws IOException
	 */
	public List<Class> listAllEventHandlerClass() throws IOException {
		if (classNames.isEmpty()) {
			System.out.println("## initEventHandler");
			initEventHandlerClassNames();
			System.out.println("## classNames = " + classNames);
		}
		if (classes.isEmpty()) {
			ClassLoader classLoader = getClass().getClassLoader();
			for (String className : classNames) {
				try {
					Class<?> clazz = classLoader.loadClass(className);
					// 实现了事件处理器接口
					if (EventHandler.class.isAssignableFrom(clazz)) {
						classes.add(clazz);
					}
				} catch (ClassNotFoundException e) {
					System.out.println(className + " ClassNotFoundException");
				}
			}
		}
		return classes;
	}

	/**
	 * 解析包路径
	 * 
	 * @param packPath
	 * @return
	 */
	private String resovleRootPath(String packPath) {
		File file = new File(getClass().getResource("/").getPath());
		String path = file.getAbsolutePath();
		String packageName = getClass().getPackage().getName().replace(".", "\\");
		path = path.replace(packageName, "");
		String packagePath = packPath.replace(".", "\\");
		path = path + "\\" + packagePath;
		return path;
	}

	/**
	 * 解析路径的类名
	 * 
	 * @param packagePath
	 * @param webPackage
	 * @return
	 */
	private void parseClassNameFromFile(String rootPath, String packagePath) {
		File root = new File(rootPath);
		resolveFile(root, packagePath);
	}

	/**
	 * 解析文件，添加类名
	 * 
	 * @param root
	 * @param webPackage
	 * @param classNames
	 */
	private void resolveFile(File root, String packagePath) {
		if (!root.exists())
			return;
		File[] childs = root.listFiles();
		if (childs != null && childs.length > 0) {
			for (File child : childs) {
				String fileName = child.getName();
				if (fileName.endsWith(".class")) {
					String name = fileName.replace(".class", "");
					String className = packagePath + "." + name;
					classNames.add(className);
				}
			}
		}
	}

}
