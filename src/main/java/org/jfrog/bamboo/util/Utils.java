package org.jfrog.bamboo.util;

import com.atlassian.bamboo.build.logger.NullBuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskExecutionContext;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

/**
 * Created by Tamirh on 26/07/2016.
 */
public class Utils {

    public static <V> Map<String, V> filterMapKeysByPrefix(Map<String, V> map, String prefix) {
        Map<String, V> result = new HashedMap();
        if (map == null) {
            return result;
        }
        for (Map.Entry<String, V> entry : map.entrySet()) {
            if (StringUtils.startsWith(entry.getKey(), prefix)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public static <V> Map<String, V> filterPropertiesKeysByPrefix(Properties properties, String prefix) {
        Map<String, V> result = new HashedMap();
        if (properties == null) {
            return result;
        }
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            V value = (V) entry.getValue();
            if (StringUtils.startsWith(key, prefix)) {
                result.put(key, value);
            }
        }
        return result;
    }

    public static void hideExternalProcessBuilderLog(TaskContext context) {
        try {
            Field buildTaskExecutionContext = context.getClass().getSuperclass().getDeclaredField("taskExecutionContext");
            buildTaskExecutionContext.setAccessible(true);
            TaskExecutionContext taskExecutionContext = (TaskExecutionContext)buildTaskExecutionContext.get(context);
            taskExecutionContext.getClass().getDeclaredField("buildLogger");
            Field buildLoggerField = taskExecutionContext.getClass().getDeclaredField("buildLogger");
            buildLoggerField.setAccessible(true);
            buildLoggerField.set(taskExecutionContext, new NullBuildLogger());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
