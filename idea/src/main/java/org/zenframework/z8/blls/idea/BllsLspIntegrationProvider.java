package org.zenframework.z8.blls.idea;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.LspClient;
import com.intellij.platform.lsp.api.LspIntegrationProvider;
import com.intellij.platform.lsp.api.lsWidget.LspClientWidgetItem;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class BllsLspIntegrationProvider implements LspIntegrationProvider {
  @Override
  public void fileOpened(
    @NonNull Project project,
    @NonNull VirtualFile virtualFile,
    LspIntegrationProvider.@NonNull LspClientStarter lspClientStarter
  ) {
    if (
      virtualFile.getFileType().equals(BlFileType.INSTANCE) &&
        BllsState.getInstance().run &&
        !BllsState.getInstance().path.isEmpty()
    ) {
      lspClientStarter.ensureClientStarted(new BllsLspClientDescriptor(project));
    }
  }

  @Override
  public @Nullable LspClientWidgetItem createWidgetItem(
    @NonNull LspClient lspClient,
    @Nullable VirtualFile currentFile
  ) {
    return new LspClientWidgetItem(lspClient, currentFile, Icons.BL, BllsConfigurable.class);
  }
}
