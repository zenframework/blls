package org.zenframework.z8.blls.idea;

import com.intellij.openapi.fileChooser.ex.FileChooserDialogImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.singleFile;

public final class BllsStartupActivity implements StartupActivity {

  @Override
  public void runActivity(@NotNull Project project) {
    var state = BllsState.getInstance();
    if ("".equals(state.path)) {
      switch (
        Messages.showDialog(
          "Не удалось найти blls",
          "blls",
          new String[]{"Указать", "Использовать PATH"},
          1,
          Messages.getErrorIcon()
        )
      ) {
        case 0 -> {
          var file = new FileChooserDialogImpl(
            singleFile().withTitle("Выберите исполняемый файл blls"),
            project
          ).choose(null);
          if (file.length == 0) return;

          state.path = file[0].getPath();
        }
        case -1, 1 -> state.path = "blls";
      }
    }
  }
}
