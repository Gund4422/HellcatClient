package me.ht9.rose.feature.registry;

import me.ht9.rose.Rose;
import me.ht9.rose.feature.Feature;
import me.ht9.rose.feature.command.Command;
import me.ht9.rose.feature.module.Module;
import me.ht9.rose.feature.module.setting.Setting;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Registry {
    private static final List<Module> modules = new ArrayList<>();
    private static final List<Command> commands = new ArrayList<>();
    private static final List<String> friends = new CopyOnWriteArrayList<>();
    private static final String prefix = ".";

    /**
     * Unified entry point. Call this in Rose.java
     */
    public static void load() {
        modules.clear();
        commands.clear();

        // Recursively scan for everything
        scan("me.ht9.rose.feature.module.modules", Module.class, modules);
        scan("me.ht9.rose.feature.command.commands", Command.class, commands);

        // Process settings and sort
        finishLoad();
    }

    private static <T> void scan(String packageName, Class<T> type, List<T> list) {
        String path = packageName.replace('.', '/');
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        try {
            Enumeration<URL> resources = loader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File dir = new File(resource.getFile().replaceAll("%20", " "));

                if (!dir.exists() || dir.listFiles() == null) continue;

                for (File file : dir.listFiles()) {
                    if (file.isDirectory()) {
                        scan(packageName + "." + file.getName(), type, list);
                    } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                        String className = packageName + "." + file.getName().replace(".class", "");
                        
                        // Skip Category file
                        if (className.endsWith("Category")) continue;

                        try {
                            Class<?> clazz = Class.forName(className);
                            if (type.isAssignableFrom(clazz) && !clazz.isInterface()) {
                                T instance = null;
                                try {
                                    // Try singleton instance() method first
                                    Method m = clazz.getDeclaredMethod("instance");
                                    m.setAccessible(true);
                                    instance = (T) m.invoke(null);
                                } catch (Exception e) {
                                    // Fallback to constructor
                                    instance = (T) clazz.getDeclaredConstructor().newInstance();
                                }
                                if (instance != null) list.add(instance);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            Rose.logger().error("Registry scan failed for: " + packageName);
        }
    }

    private static void finishLoad() {
        modules.forEach(module -> {
            // Automatically find Setting fields in the module class via reflection
            for (Field field : module.getClass().getDeclaredFields()) {
                if (Setting.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        module.settings().add((Setting<?>) field.get(module));
                    } catch (Throwable ignored) {}
                }
            }
            // Add global/base settings
            module.settings().add(module.drawn());
            module.settings().add(module.bindMode());
            module.settings().add(module.toggleBind());
        });

        // Alphabetize for the GUI list
        modules.sort(Comparator.comparing(Feature::name));
        commands.sort(Comparator.comparing(Command::name));
        
        Rose.logger().info("Hose/Rose Registry: " + modules.size() + " Modules, " + commands.size() + " Commands.");
    }

    public static List<Module> modules() { return modules; }
    public static List<Command> commands() { return commands; }
    public static List<String> friends() { return friends; }
    public static String prefix() { return prefix; }
}
