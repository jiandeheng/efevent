package com.efun.efevent.event;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.efun.efevent.event.EventHandler;

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

	private void initEventHandler() {
		String rootPath = resovleRootPath(PACKAGE_PATH);
		parseClassName(rootPath, PACKAGE_PATH);
	}

	/**
	 * 获取事件处理器的类集合
	 * 
	 * @return
	 */
	public List<Class> listAllEventHandlerClass() {
		if (classNames.isEmpty()) {
			initEventHandler();
		}
		if (classes.isEmpty()) {
			ClassLoader classLoader = getClass().getClassLoader();
			for (String className : classNames) {
				try {
					Class clazz = classLoader.loadClass(className);
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
	private void parseClassName(String rootPath, String packagePath) {
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
