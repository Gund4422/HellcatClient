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

    public static void load() {
        modules.clear();
        commands.clear();

        // 1. Auto-load Modules
        scan("me.ht9.rose.feature.module.modules", Module.class, modules);
        
        // 2. Auto-load Commands
        scan("me.ht9.rose.feature.command.commands", Command.class, commands);

        // 3. Setup Settings and Sorting
        finishLoad();
    }

    private static <T> void scan(String packageName, Class<T> type, List<T> list) {
        try {
            String path = packageName.replace('.', '/');
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = loader.getResources(path);
            
            while (resources.hasMoreElements()) {
                File dir = new File(resources.nextElement().getFile());
                if (!dir.exists()) continue;
                
                for (File file : Objects.requireNonNull(dir.listFiles())) {
                    if (file.isDirectory()) {
                        scan(packageName + "." + file.getName(), type, list);
                    } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                        String className = packageName + "." + file.getName().replace(".class", "");
                        if (className.endsWith("Category")) continue;

                        Class<?> clazz = Class.forName(className);
                        if (type.isAssignableFrom(clazz) && !clazz.isInterface()) {
                            T instance = null;
                            try {
                                // Try Rose singleton pattern
                                Method m = clazz.getDeclaredMethod("instance");
                                m.setAccessible(true);
                                instance = (T) m.invoke(null);
                            } catch (Exception e) {
                                // Fallback to no-args constructor
                                instance = (T) clazz.getDeclaredConstructor().newInstance();
                            }
                            if (instance != null) list.add(instance);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Rose.logger().error("Registry failed to scan " + packageName);
        }
    }

    private static void finishLoad() {
        modules.forEach(module -> {
            for (Field field : module.getClass().getDeclaredFields()) {
                if (Setting.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        module.settings().add((Setting<?>) field.get(module));
                    } catch (Throwable ignored) {}
                }
            }
            module.settings().add(module.drawn());
            module.settings().add(module.bindMode());
            module.settings().add(module.toggleBind());
        });

        modules.sort(Comparator.comparing(Feature::name));
        commands.sort(Comparator.comparing(Command::name));
        Rose.logger().info("Rose Registry: Loaded " + modules.size() + " modules and " + commands.size() + " commands.");
    }

    public static List<Module> modules() { return modules; }
    public static List<Command> commands() { return commands; }
    public static List<String> friends() { return friends; }
    public static String prefix() { return prefix; }
}
