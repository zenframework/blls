package org.zenframework.z8.blls.evalsql;

public abstract class VisitorCheckSupported extends Visitor {
  protected abstract DatabaseVendor vendor();

  /// `token`.name: операция не поддерживается
  protected abstract void diagnosticDatabaseUnsupported(SqlToken token);

  @Override
  public void visit(int id) {
    super.visit(id);

    if (this.token != null) try {
      this.token.format(vendor(), new FormatOptions());
    } catch (SqlToken.UnknownDatabaseException e) {
      diagnosticDatabaseUnsupported(e.getToken());
    } catch (Uneval.Exception _) {
    }
    this.token = null;
  }
}
