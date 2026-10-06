package org.zenframework.z8.blls.idea;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.singleDir;
import static com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.singleFile;

public final class BllsConfigurable implements Configurable {

  private JPanel panel;
  private JBCheckBox run;
  private TextFieldWithBrowseButton path;
  private TextFieldWithBrowseButton z8;
  private JBCheckBox record;
  private ComboBox<BllsState.DatabaseVendor> databaseVendor;
  private JBCheckBox inlayHintBoolean;
  private JBCheckBox inlayHintNonObvious;
  private JBCheckBox inlayHintImplicit;
  private JBCheckBox codeLensSql;

  @Override
  public String getDisplayName() {
    return "blls";
  }

  @Override
  public @Nullable JComponent createComponent() {
    run = new JBCheckBox("Запускать языковой сервер");
    path = new TextFieldWithBrowseButton();
    path.addBrowseFolderListener(null, singleFile().withTitle("Выберите исполняемый файл blls"));
    z8 = new TextFieldWithBrowseButton();
    z8.addBrowseFolderListener(null, singleDir().withTitle("Выберите папку z8"));
    record = new JBCheckBox("Записывать запросы");
    databaseVendor = new ComboBox<>(BllsState.DatabaseVendor.values());
    inlayHintBoolean = new JBCheckBox(
      "Вложенные подсказки: обозначать параметры типа bool"
    );
    inlayHintNonObvious = new JBCheckBox(
      "Вложенные подсказки: обозначать параметр, если его название не очевидно"
    );
    inlayHintImplicit = new JBCheckBox(
      "Вложенные подсказки: обозначать неявные вызовы"
    );
    codeLensSql = new JBCheckBox("Просчитывать SQL-запросы");

    panel = FormBuilder.createFormBuilder()
      .addComponent(run, 1)
      .addLabeledComponent("Путь к blls", path, 1, false)
      .addLabeledComponent("Путь к z8", z8, 1, false)
      .addComponent(record, 1)
      .addLabeledComponent("База данных", databaseVendor, 1, false)
      .addComponent(inlayHintBoolean, 1)
      .addComponent(inlayHintNonObvious, 1)
      .addComponent(inlayHintImplicit, 1)
      .addComponent(codeLensSql, 1)
      .addComponentFillVertically(new JPanel(), 0)
      .getPanel();

    return panel;
  }

  @Override
  public boolean isModified() {
    var state = BllsState.getInstance();
    boolean modified = false;
    modified |= state.run != run.isSelected();
    modified |= !state.path.equals(path.getText());
    modified |= !state.z8.equals(z8.getText());
    modified |= state.record != record.isSelected();
    modified |= state.databaseVendor != databaseVendor.getItem();
    modified |= state.inlayHintBoolean != inlayHintBoolean.isSelected();
    modified |= state.inlayHintNonObvious != inlayHintNonObvious.isSelected();
    modified |= state.inlayHintImplicit != inlayHintImplicit.isSelected();
    modified |= state.codeLensSql != codeLensSql.isSelected();
    return modified;
  }

  @Override
  public void apply() {
    var state = BllsState.getInstance();
    state.run = run.isSelected();
    state.path = path.getText();
    state.z8 = z8.getText();
    state.record = record.isSelected();
    state.databaseVendor = databaseVendor.getItem();
    state.inlayHintBoolean = inlayHintBoolean.isSelected();
    state.inlayHintNonObvious = inlayHintNonObvious.isSelected();
    state.inlayHintImplicit = inlayHintImplicit.isSelected();
    state.codeLensSql = codeLensSql.isSelected();
    BllsState.fireStateChanged();
  }

  @Override
  public void reset() {
    var state = BllsState.getInstance();
    run.setSelected(state.run);
    path.setText(state.path);
    z8.setText(state.z8);
    record.setSelected(state.record);
    databaseVendor.setItem(state.databaseVendor);
    inlayHintBoolean.setSelected(state.inlayHintBoolean);
    inlayHintNonObvious.setSelected(state.inlayHintNonObvious);
    inlayHintImplicit.setSelected(state.inlayHintImplicit);
    codeLensSql.setSelected(state.codeLensSql);
  }

  @Override
  public void disposeUIResources() {
    panel = null;
    run = null;
    path = null;
    z8 = null;
    record = null;
    databaseVendor = null;
    inlayHintBoolean = null;
    inlayHintNonObvious = null;
    inlayHintImplicit = null;
    codeLensSql = null;
  }
}
