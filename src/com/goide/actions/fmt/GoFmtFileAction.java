package com.goide.actions.fmt;

import com.goide.GoSdkType;
import com.goide.jps.model.JpsGoSdkType;
import com.goide.psi.GoFile;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class GoFmtFileAction extends AnAction implements DumbAware {
  private static final String NOTIFICATION_TITLE = "Reformat code with go gmt";
  private static final Logger LOG = Logger.getInstance(GoFmtFileAction.class);

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(e.getProject() != null && e.getData(CommonDataKeys.PSI_FILE) instanceof GoFile);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
    final Project project = e.getProject();
    assert project != null;
    assert file instanceof GoFile;
    final VirtualFile vFile = file.getVirtualFile();

    final String groupId = e.getPresentation().getText();

    try {
      doFmt(file, project, vFile, groupId);
    }
    catch (Exception ex) {
      error(file, project, groupId, ex);
      LOG.error(ex);
    }
  }

  public static boolean doFmt(@NotNull PsiFile file, @NotNull Project project, @Nullable final VirtualFile vFile, @Nullable String groupId)
    throws ExecutionException {
    if (vFile == null || !vFile.isInLocalFileSystem()) return true;
    Document document = PsiDocumentManager.getInstance(project).getDocument(file);
    assert document != null;
    String filePath = vFile.getCanonicalPath();
    assert filePath != null;

    GeneralCommandLine commandLine = new GeneralCommandLine();
    Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();

    String sdkHome = sdk != null ? sdk.getHomePath() : null;
    if (StringUtil.isEmpty(sdkHome)) {
      if (groupId != null) warning(project, groupId, "Project sdk is empty");
      return true;
    }
    if (!(sdk.getSdkType() instanceof GoSdkType)) {
      if (groupId != null) warning(project, groupId, "Project sdk is not valid");
      return true;
    }

    File executable = JpsGoSdkType.getGoExecutableFile(sdkHome);

    commandLine.setExePath(executable.getAbsolutePath());
    commandLine.addParameters("fmt", filePath);

    FileDocumentManager.getInstance().saveDocument(document);

    String commandLineString = commandLine.getCommandLineString();
    OSProcessHandler handler = new OSProcessHandler(commandLine.createProcess(), commandLineString);
    handler.addProcessListener(new ProcessAdapter() {
      @Override
      public void processTerminated(ProcessEvent event) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
          @Override
          public void run() {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
              @Override
              public void run() {
                vFile.refresh(false, false);
              }
            });
          }
        });
      }
    });
    handler.startNotify();
    return false;
  }

  private static void error(@NotNull PsiFile file, @NotNull Project project, @NotNull String groupId, @Nullable Exception ex) {
    Notifications.Bus.notify(new Notification(groupId, file.getName() + " formatting with go fmt failed",
                                              ex == null ? "" : ExceptionUtil.getUserStackTrace(ex, LOG),
                                              NotificationType.ERROR), project);
  }

  private static void warning(@NotNull Project project, @NotNull String groupId, @NotNull String content) {
    Notifications.Bus.notify(new Notification(groupId, NOTIFICATION_TITLE, content, NotificationType.WARNING), project);
  }
}
