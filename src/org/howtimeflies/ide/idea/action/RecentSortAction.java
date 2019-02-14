package org.howtimeflies.ide.idea.action;

import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhanggeng on 2017/11/9.
 *
 * @author <a href="mailto:ujjboy@qq.com">zhanggeng</a>
 */
public class RecentSortAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(RecentSortAction.class);

    static Field myNameCacheField;

    static {
        try {
            myNameCacheField = RecentProjectsManagerBase.class.getDeclaredField("myNameCache");
            myNameCacheField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            String[] fieldNames = "a,b,c,d,e,f,g".split(","); // 代码被混淆，挨个找
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
            LOG.info("启动历史项目监听线程!");
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    RecentProjectsManagerBase base = (RecentProjectsManagerBase) RecentProjectsManager.getInstance();
                    RecentProjectsManagerBase.State state = base.getState();
                    //  System.out.println("===========");
                    try {
                        Map<String, String> map = (Map<String, String>) myNameCacheField.get(base);
                        /* for (Map.Entry<String, String> entry : map.entrySet()) {
                            System.out.println(entry.getKey() + ":" + entry.getValue());
                        }
                        System.out.println("-----");*/
                        for (String recentPath : state.recentPaths) {
                            LOG.info(recentPath);
                        }
                        LOG.info("---before sort---");
                        sortRecentPaths(map, state.recentPaths);

                       /* for (Map.Entry<String, String> entry : newMap.entrySet()) {
                            System.out.println(entry.getKey() + ":" + entry.getValue());
                        }
                        System.out.println("-----");*/
                        for (String recentPath : state.recentPaths) {
                            LOG.info(recentPath);
                        }
                        LOG.info("---after sort---");
                    } catch (Throwable e) {
                        LOG.error("Sort recent project error !", e);
                    }
                }
            }, 20, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {

    }

    private static void sortRecentPaths(Map<String, String> map, List<String> recentPaths) {
        recentPaths.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String p1 = map.get(o1);
                String p2 = map.get(o2);
                return p1 == null ?
                        (p2 == null ? 0 : -1)
                        : (p2 == null ? 1 : p1.compareTo(p2));
            }
        });
    }
}

