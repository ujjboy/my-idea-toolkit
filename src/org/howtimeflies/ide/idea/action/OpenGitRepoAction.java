package org.howtimeflies.ide.idea.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;

/**
 * @author <a href="mailto:zhanggeng.zg@antfin.com">GengZhang</a>
 */
public class OpenGitRepoAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(OpenGitRepoAction.class);

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getData(LangDataKeys.PROJECT);
        if (project != null) {
            String path = project.getBasePath();
            if (path != null) {
                if (new File(path, ".git").exists()) {
                    // only visible on git project
                    e.getPresentation().setVisible(true);
                    return;
                }
            }
        }
        e.getPresentation().setVisible(false);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getData(LangDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        String projectBasePath = project.getBasePath();
        if (projectBasePath == null) {
            return;
        }
        File gitDir = new File(projectBasePath, ".git");
        if (!gitDir.exists()) {
            return;
        }
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

        // parse git id
        String gitId;
        String fetchHead = file2String(new File(gitDir, "FETCH_HEAD"));
        String head = file2String(new File(gitDir, "HEAD"));
        if (head == null) {
            return;
        } else {
            String branch = head.substring(16);
            if (fetchHead.contains("'" + branch + "'")) {
                // exist remote branch
                gitId = branch;
            } else {
                // use log id
                gitId = file2String(new File(gitDir, head.substring(5).trim()));
            }
        }
        if (gitId == null) {
            return;
        }
        String config = file2String(new File(gitDir, "config"));
        if (config == null) {
            return;
        }

        // parse git base url
        String gitBaseUrl;
        int index = config.indexOf("url = ");
        if (index < 0) {
            index = config.indexOf("url =");
            if (index < 0) {
                index = config.indexOf("url=");
                if (index < 0) {
                    return;
                } else {
                    gitBaseUrl = config.substring(index + 4);
                }
            } else {
                gitBaseUrl = config.substring(index + 5);
            }
        } else {
            gitBaseUrl = config.substring(index + 6);
        }
        gitBaseUrl = gitBaseUrl.substring(0, gitBaseUrl.indexOf(".git"));
        gitBaseUrl = gitBaseUrl.replace(":", "/");
        gitBaseUrl = gitBaseUrl.replace("git://", "https://");
        gitBaseUrl = gitBaseUrl.replace("git@", "https://");

        // parse relative path
        String relativePath = path.replace(projectBasePath, "");
        relativePath = relativePath.startsWith("/") ? relativePath : "/" + relativePath;

        // build git page url
        // https://github.com/ujjboy/sofa-rpc-extension-demo/blob/ebfbe07502149181af828e5f34bd4323cc4c0aea/src/main/java/org/howtimeflies/sofa/rpc/registry/MyRegistry.java
        String gitPage = gitBaseUrl + "/tree/" + gitId + relativePath;
        LOG.info("open github page: " + gitPage);
        doCommand(getOpenCommand(gitPage));
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

    /**
     * 读取文件内容
     *
     * @param file 文件
     * @return 文件内容
     * @throws IOException 发送IO异常
     */
    public String file2String(File file) {
        if (file == null || !file.exists() || !file.isFile() || !file.canRead()) {
            return null;
        }
        try {
            FileReader reader = null;
            StringWriter writer = null;
            try {
                reader = new FileReader(file);
                writer = new StringWriter();
                char[] cbuf = new char[1024];
                int len = 0;
                while ((len = reader.read(cbuf)) != -1) {
                    writer.write(cbuf, 0, len);
                }
                return writer.toString().trim();
            } finally {
                closeQuietly(reader);
                closeQuietly(writer);
            }
        } catch (Exception e) {
            LOG.error("Read file error!", e);
        }
        return null;
    }

    /**
     * 静默关闭
     *
     * @param closeable 可关闭的
     */
    public void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignore) {
                // NOPMD
            }
        }
    }
}
