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
     * The only method you need to call. 
     * Scans everything, instantiates, and wires up settings.
     */
    public static void load() {
        modules.clear();
        commands.clear();

        // Auto-scan Modules and Commands
        scan("me.ht9.rose.feature.module.modules", Module.class, modules);
        scan("me.ht9.rose.feature.command.commands", Command.class, commands);

        // Run the internal setup
        finishLoad();
    }

    private static <T> void scan(String packageName, Class<T> type, List<T> list) {
        try {
            String path = packageName.replace('.', '/');
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = loader.getResources(path);
            
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File dir = new File(resource.getFile().replaceAll("%20", " ")); // Fix spaces in paths
                
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
                                // Priority 1: .instance()
                                Method m = clazz.getDeclaredMethod("instance");
                                m.setAccessible(true);
                                instance = (T) m.invoke(null);
                            } catch (Exception e) {
                                // Priority 2: Constructor
                                try {
                                    instance = (T) clazz.getDeclaredConstructor().newInstance();
                                } catch (Exception ignored) {}
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
            // Use reflection to find all Setting fields in the module class
            for (Field field : module.getClass().getDeclaredFields()) {
                if (Setting.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        module.settings().add((Setting<?>) field.get(module));
                    } catch (Throwable ignored) {}
                }
            }
            // Add the hardcoded/base settings
            module.settings().add(module.drawn());
            module.settings().add(module.bindMode());
            module.settings().add(module.toggleBind());
        });

        // Alphabetize for the GUI
        modules.sort(Comparator.comparing(Feature::name));
        commands.sort(Comparator.comparing(Command::name));
        
        Rose.logger().info("Hose Registry: Initialized " + modules.size() + " modules and " + commands.size() + " commands.");
    }

    public static List<Module> modules() { return modules; }
    public static List<Command> commands() { return commands; }
    public static List<String> friends() { return friends; }
    public static String prefix() { return prefix; }
}
