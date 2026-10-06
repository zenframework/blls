package org.zenframework.z8.blls.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@State(
  name = "org.zenframework.z8.blls.idea.BllsState",
  storages = @Storage("blls.xml")
)
public final class BllsState implements PersistentStateComponent<BllsState> {

  private static final List<Runnable> listeners = ContainerUtil.createConcurrentList();
  public boolean run = true;
  public String path = "";
  public String z8 = "";
  public boolean record = false;
  public DatabaseVendor databaseVendor = DatabaseVendor.Postgres;
  public boolean inlayHintBoolean = true;
  public boolean inlayHintNonObvious = true;
  public boolean inlayHintImplicit = false;
  public boolean codeLensSql = true;

  public static BllsState getInstance() {
    return ApplicationManager.getApplication().getService(BllsState.class);
  }

  public static void addListener(Runnable listener) {
    listeners.add(listener);
  }

  public static void removeListener(Runnable listener) {
    listeners.remove(listener);
  }

  public static void fireStateChanged() {
    listeners.forEach(Runnable::run);
  }

  @Override
  public @NotNull BllsState getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull BllsState state) {
    XmlSerializerUtil.copyBean(state, this);
    fireStateChanged();
  }

  public enum DatabaseVendor {
    Oracle,
    SqlServer,
    Postgres,
    H2,
  }
}
