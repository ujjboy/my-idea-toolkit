package org.howtimeflies.ide.idea.action;

import com.intellij.ide.RecentProjectManagerState;
import com.intellij.ide.RecentProjectMetaInfo;
import com.intellij.ide.RecentProjectsManager;
import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
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
            myNameCacheField = RecentProjectsManagerBase.class.getDeclaredField("nameCache");
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
                    // https://github.com/JetBrains/intellij-community/blob/master/platform/platform-impl/src/com/intellij/ide/RecentProjectsManagerBase.kt
                    RecentProjectsManagerBase base = (RecentProjectsManagerBase) RecentProjectsManager.getInstance();
                    // https://github.com/JetBrains/intellij-community/blob/master/platform/platform-impl/src/com/intellij/ide/RecentProjectMetaInfo.kt#L39
                    RecentProjectManagerState state = base.getState();
                    //  System.out.println("===========");
                    try {
                        Map<String, String> nameCache = (Map<String, String>) myNameCacheField.get(base);
                        /* for (Map.Entry<String, String> entry : map.entrySet()) {
                            System.out.println(entry.getKey() + ":" + entry.getValue());
                        }
                        System.out.println("-----");*/
                        // linkedMap<String, RecentProjectMetaInfo>()
                        LOG.info("---before sort---");
                        Map<String, RecentProjectMetaInfo> map1 = state.getAdditionalInfo();
                        for (Map.Entry<String, RecentProjectMetaInfo> entry : map1.entrySet()) {
                            LOG.info(entry.getKey() + " --> " + entry.getValue());
                        }
                        LOG.info("---do sort---");
                        sortRecentPaths(nameCache, map1);

                       /* for (Map.Entry<String, String> entry : newMap.entrySet()) {
                            System.out.println(entry.getKey() + ":" + entry.getValue());
                        }
                        System.out.println("-----");*/
                        LOG.info("---after sort---");
                        for (Map.Entry<String, RecentProjectMetaInfo> entry : map1.entrySet()) {
                            LOG.info(entry.getKey() + " --> " + entry.getValue());
                        }
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

    private static void sortRecentPaths(Map<String, String> map, Map<String, RecentProjectMetaInfo> map1) {

        //先转成ArrayList集合
        ArrayList<Map.Entry<String, RecentProjectMetaInfo>> list = new ArrayList<>(map1.entrySet());

        //从小到大排序（从大到小将o1与o2交换即可）
        Collections.sort(list, (o1, o2) -> {
            String p1 = map.get(o1.getKey());
            String p2 = map.get(o2.getKey());
            return p1 == null ?
                    (p2 == null ? 0 : 1)
                    : (p2 == null ? -1 : p2.compareTo(p1));
        });

        map1.clear();
        for (Map.Entry<String, RecentProjectMetaInfo> entry : list) {
            map1.put(entry.getKey(), entry.getValue());
        }
    }
}

