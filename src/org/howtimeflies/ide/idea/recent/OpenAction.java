package org.howtimeflies.ide.idea.recent;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;

import java.io.IOException;

/**
 * Created by zhanggeng on 2018/3/29.
 *
 * @author <a href="mailto:ujjboy@qq.com">zhanggeng</a>
 */
public class OpenAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(AnAction.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        String path = null;
        try {
            PsiFile file = e.getDataContext().getData(DataKeys.PSI_FILE);
            if (file == null) {
                PsiDirectory directory = (PsiDirectory) e.getDataContext().getData(DataKeys.PSI_ELEMENT);
                path = directory.getVirtualFile().getPath();
            } else {
                VirtualFile vf = file.getVirtualFile();
                path = vf.getPath();
            }
        } catch (Exception ex) {
            LOG.error("", ex);
        }
        if (path == null) {
            return;
        }
        doCommand(getOpenCommand(path));
    }

    private void doCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            LOG.error("Failed to run [" + command + "]", e);
        }
    }

    private String getOpenCommand(String path) {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            return path;
        } else if (osName.contains("linux")) {
            return "open " + path;
        } else if (osName.contains("mac")) {
            return "open " + path;
        }
        return path;
    }
}
