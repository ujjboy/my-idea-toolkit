package io.bsoa.ide.idea.recent;

import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.openapi.actionSystem.AnActionEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhanggeng on 2017/11/9.
 *
 * @author <a href="mailto:zhanggeng.zg@antfin.com">zhanggeng</a>
 */
public class RecentAction extends com.intellij.openapi.actionSystem.AnAction {

    static Field myNameCacheField;

    static {
        try {
            myNameCacheField = RecentProjectsManagerBase.class.getDeclaredField("myNameCache");
            myNameCacheField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            String[] fieldNames = "a,b,c,d,e".split(",");
            for (String s : fieldNames) {
                try {
                    Field field = RecentProjectsManagerBase.class.getDeclaredField(s);
                    field.setAccessible(true);
                    if (Map.class.isAssignableFrom(field.getType())) {
                        myNameCacheField = field;
                        break;
                    }
                } catch (NoSuchFieldException e1) {
                }
            }
        }
        if (myNameCacheField != null && myNameCacheField.isAccessible()) {
            System.out.println("启动历史项目监听线程!");
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    RecentProjectsManagerBase base = (RecentProjectsManagerBase) RecentProjectsManager.getInstance();
                    RecentProjectsManagerBase.State state = base.getState();
                    try {
                        Map<String, String> map = (Map<String, String>) myNameCacheField.get(base);
                        Map<String, String> newMap = convertToSortedMap(map, state.recentPaths);
                        myNameCacheField.set(base, newMap);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }, 20, 10, TimeUnit.SECONDS);
        }
    }
    
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
       
    }

    private static Map<String, String> convertToSortedMap(Map<String, String> map, List<String> recentPaths) {
        List<Map.Entry<String,String>> list = new ArrayList<Map.Entry<String,String>>(map.entrySet());
        Collections.sort(list,new Comparator<Map.Entry<String,String>>() {
            //升序排序
            public int compare(Map.Entry<String, String> o1,
                               Map.Entry<String, String> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        Map<String, String> newMap = Collections.synchronizedMap(new TreeMap<>());
        recentPaths.clear();
        for (Map.Entry<String, String> entry : list) {
            newMap.put(entry.getKey(), entry.getValue());
            recentPaths.add(entry.getKey());
        }
        return newMap;
    }
}

