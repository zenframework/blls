package org.zenframework.z8.blls.idea;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.ConfigurationItem;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BllsLspClientDescriptor extends ProjectWideLspClientDescriptor {
  protected BllsLspClientDescriptor(@NonNull Project project) {
    super(project, "blls");
  }

  @Override
  public boolean isSupportedFile(@NonNull VirtualFile virtualFile) {
    return virtualFile.getFileType().equals(BlFileType.INSTANCE);
  }

  @Override
  public @NonNull ClientCapabilities getClientCapabilities() {
    var result = super.getClientCapabilities();
    result.getWorkspace().setConfiguration(true);
    // https://github.com/AleksandrSl/intellij-luau/blob/ac249d5d48ef69c078cc68fcc3b37e01200b5125/src/main/kotlin/com/github/aleksandrsl/intellijluau/lsp/LuauLspServerSupportProvider.kt#L75
    result.getTextDocument().setDiagnostic(null);
    return result;
  }

  @Override
  public @Nullable Object createInitializationOptions() {
    var state = BllsState.getInstance();
    return new Object() {
      public final String databaseVendor = state.databaseVendor.name();
      public final boolean inlayHintBoolean = state.inlayHintBoolean;
      public final boolean inlayHintNonObvious = state.inlayHintNonObvious;
      public final boolean inlayHintImplicit = state.inlayHintImplicit;
      public final boolean codeLensSql = state.codeLensSql;
    };
  }

  @Override
  public @Nullable Object getWorkspaceConfiguration(@NonNull ConfigurationItem item) {
    var state = BllsState.getInstance();
    return switch (item.getSection()) {
      case "blls.databaseVendor" -> state.databaseVendor.name();
      case "blls.inlayHintBoolean" -> state.inlayHintBoolean;
      case "blls.inlayHintNonObvious" -> state.inlayHintNonObvious;
      case "blls.inlayHintImplicit" -> state.inlayHintImplicit;
      case "blls.codeLensSql" -> state.codeLensSql;
      default -> null;
    };
  }

  @Override
  public @NonNull GeneralCommandLine createCommandLine() {
    var state = BllsState.getInstance();
    var args = new ArrayList<String>();
    args.add(state.path);

    String z8 = state.z8;
    if (!"".equals(z8)) args.addAll(List.of("--z8", z8));
    if (state.record) args.add("--record");

    return new GeneralCommandLine(args);
  }
}
