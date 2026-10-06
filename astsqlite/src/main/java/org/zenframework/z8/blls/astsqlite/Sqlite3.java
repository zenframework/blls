package org.zenframework.z8.blls.astsqlite;

import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Optional;

import static java.lang.foreign.MemoryLayout.sequenceLayout;
import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

public final class Sqlite3 {
  private static final int OK = 0;
  private static final int ERROR = 1;
  private static final int INTERNAL = 2;
  private static final int PERM = 3;
  private static final int ABORT = 4;
  private static final int BUSY = 5;
  private static final int LOCKED = 6;
  private static final int NOMEM = 7;
  private static final int READONLY = 8;
  private static final int INTERRUPT = 9;
  private static final int IOERR = 10;
  private static final int CORRUPT = 11;
  private static final int NOTFOUND = 12;
  private static final int FULL = 13;
  private static final int CANTOPEN = 14;
  private static final int PROTOCOL = 15;
  private static final int EMPTY = 16;
  private static final int SCHEMA = 17;
  private static final int TOOBIG = 18;
  private static final int CONSTRAINT = 19;
  private static final int MISMATCH = 20;
  private static final int MISUSE = 21;
  private static final int NOLFS = 22;
  private static final int AUTH = 23;
  private static final int FORMAT = 24;
  private static final int RANGE = 25;
  private static final int NOTADB = 26;
  private static final int NOTICE = 27;
  private static final int WARNING = 28;
  private static final int ROW = 100;
  private static final int DONE = 101;
  private final ValueLayout.OfInt INT;
  private final AddressLayout POINTER;
  private final MethodHandle sqlite3_libversion;
  private final MethodHandle sqlite3_sourceid;
  private final MethodHandle sqlite3_config;
  private final MethodHandle sqlite3_close;
  private final MethodHandle sqlite3_close_v2;
  private final MethodHandle sqlite3_exec;
  private final MethodHandle sqlite3_initialize;
  private final MethodHandle sqlite3_serialize;
  private final MethodHandle sqlite3_shutdown;
  private final MethodHandle sqlite3_changes64;
  private final MethodHandle sqlite3_free;
  private final MethodHandle sqlite3_open_v2;
  private final MethodHandle sqlite3_errcode;
  private final MethodHandle sqlite3_errmsg;
  private final MethodHandle sqlite3_errstr;
  private final MethodHandle sqlite3_prepare_v3;
  private final MethodHandle sqlite3_bind_double;
  private final MethodHandle sqlite3_bind_blob;
  private final MethodHandle sqlite3_bind_int64;
  private final MethodHandle sqlite3_bind_null;
  private final MethodHandle sqlite3_bind_text;
  private final MethodHandle sqlite3_bind_parameter_count;
  private final MethodHandle sqlite3_clear_bindings;
  private final MethodHandle sqlite3_column_count;
  private final MethodHandle sqlite3_step;
  private final MethodHandle sqlite3_data_count;
  private final MethodHandle sqlite3_column_double;
  private final MethodHandle sqlite3_column_blob;
  private final MethodHandle sqlite3_column_int64;
  private final MethodHandle sqlite3_column_text;
  private final MethodHandle sqlite3_column_bytes;
  private final MethodHandle sqlite3_finalize;
  private final MethodHandle sqlite3_reset;
  private final MethodHandle sqlite3_db_handle;

  public Sqlite3(Linker linker, SymbolLookup lookup) {
    var cl = linker.canonicalLayouts();

    var CHAR = (ValueLayout.OfByte) cl.get("char");
    INT = (ValueLayout.OfInt) cl.get("int");
    var DOUBLE = (ValueLayout.OfDouble) cl.get("double");
    POINTER = ((AddressLayout) cl.get("void*")).withTargetLayout(sequenceLayout(Long.MAX_VALUE, CHAR));

    sqlite3_libversion = linker.downcallHandle(lookup.findOrThrow("sqlite3_libversion"), FunctionDescriptor.of(POINTER));
    sqlite3_sourceid = linker.downcallHandle(lookup.findOrThrow("sqlite3_sourceid"), FunctionDescriptor.of(POINTER));
    sqlite3_config = linker.downcallHandle(lookup.findOrThrow("sqlite3_config"), FunctionDescriptor.of(INT, INT, INT), Linker.Option.firstVariadicArg(1));
    sqlite3_close = linker.downcallHandle(lookup.findOrThrow("sqlite3_close"), FunctionDescriptor.of(INT, POINTER));
    sqlite3_close_v2 = linker.downcallHandle(lookup.findOrThrow("sqlite3_close"), FunctionDescriptor.of(INT, POINTER));
    sqlite3_exec = linker.downcallHandle(lookup.findOrThrow("sqlite3_exec"), FunctionDescriptor.of(INT, POINTER, POINTER, POINTER, POINTER, POINTER));
    sqlite3_initialize = linker.downcallHandle(lookup.findOrThrow("sqlite3_initialize"), FunctionDescriptor.of(INT));
    sqlite3_serialize = linker.downcallHandle(lookup.findOrThrow("sqlite3_serialize"), FunctionDescriptor.of(POINTER, POINTER, POINTER, POINTER, INT));
    sqlite3_shutdown = linker.downcallHandle(lookup.findOrThrow("sqlite3_shutdown"), FunctionDescriptor.of(INT));
    sqlite3_changes64 = linker.downcallHandle(lookup.findOrThrow("sqlite3_changes64"), FunctionDescriptor.of(JAVA_LONG, POINTER));
    sqlite3_free = linker.downcallHandle(lookup.findOrThrow("sqlite3_free"), FunctionDescriptor.ofVoid(POINTER));
    sqlite3_open_v2 = linker.downcallHandle(lookup.findOrThrow("sqlite3_open_v2"), FunctionDescriptor.of(INT, POINTER, POINTER, INT, POINTER));
    sqlite3_errcode = linker.downcallHandle(lookup.findOrThrow("sqlite3_errcode"), FunctionDescriptor.of(INT, POINTER));
    sqlite3_errmsg = linker.downcallHandle(lookup.findOrThrow("sqlite3_errmsg"), FunctionDescriptor.of(POINTER, POINTER));
    sqlite3_errstr = linker.downcallHandle(lookup.findOrThrow("sqlite3_errstr"), FunctionDescriptor.of(POINTER, INT));
    sqlite3_prepare_v3 = linker.downcallHandle(lookup.findOrThrow("sqlite3_prepare_v3"), FunctionDescriptor.of(INT, POINTER, POINTER, INT, INT, POINTER, POINTER));
    sqlite3_bind_double = linker.downcallHandle(lookup.findOrThrow("sqlite3_bind_double"), FunctionDescriptor.of(INT, POINTER, INT, DOUBLE));
    sqlite3_bind_blob = linker.downcallHandle(lookup.findOrThrow("sqlite3_bind_blob"), FunctionDescriptor.of(INT, POINTER, INT, POINTER, INT, POINTER));
    sqlite3_bind_int64 = linker.downcallHandle(lookup.findOrThrow("sqlite3_bind_int64"), FunctionDescriptor.of(INT, POINTER, INT, JAVA_LONG));
    sqlite3_bind_null = linker.downcallHandle(lookup.findOrThrow("sqlite3_bind_null"), FunctionDescriptor.of(INT, POINTER, INT));
    sqlite3_bind_text = linker.downcallHandle(lookup.findOrThrow("sqlite3_bind_text"), FunctionDescriptor.of(INT, POINTER, INT, POINTER, INT, POINTER));
    sqlite3_bind_parameter_count = linker.downcallHandle(lookup.findOrThrow("sqlite3_bind_parameter_count"), FunctionDescriptor.of(INT, POINTER));
    sqlite3_clear_bindings = linker.downcallHandle(lookup.findOrThrow("sqlite3_clear_bindings"), FunctionDescriptor.of(INT, POINTER));
    sqlite3_column_count = linker.downcallHandle(lookup.findOrThrow("sqlite3_column_count"), FunctionDescriptor.of(INT, POINTER));
    sqlite3_step = linker.downcallHandle(lookup.findOrThrow("sqlite3_step"), FunctionDescriptor.of(INT, POINTER));
    sqlite3_data_count = linker.downcallHandle(lookup.findOrThrow("sqlite3_data_count"), FunctionDescriptor.of(INT, POINTER));
    sqlite3_column_double = linker.downcallHandle(lookup.findOrThrow("sqlite3_column_double"), FunctionDescriptor.of(DOUBLE, POINTER, INT));
    sqlite3_column_blob = linker.downcallHandle(lookup.findOrThrow("sqlite3_column_blob"), FunctionDescriptor.of(POINTER, POINTER, INT));
    sqlite3_column_int64 = linker.downcallHandle(lookup.findOrThrow("sqlite3_column_int64"), FunctionDescriptor.of(JAVA_LONG, POINTER, INT));
    sqlite3_column_text = linker.downcallHandle(lookup.findOrThrow("sqlite3_column_text"), FunctionDescriptor.of(POINTER, POINTER, INT));
    sqlite3_column_bytes = linker.downcallHandle(lookup.findOrThrow("sqlite3_column_bytes"), FunctionDescriptor.of(INT, POINTER, INT));
    sqlite3_finalize = linker.downcallHandle(lookup.findOrThrow("sqlite3_finalize"), FunctionDescriptor.of(INT, POINTER));
    sqlite3_reset = linker.downcallHandle(lookup.findOrThrow("sqlite3_reset"), FunctionDescriptor.of(INT, POINTER));
    sqlite3_db_handle = linker.downcallHandle(lookup.findOrThrow("sqlite3_db_handle"), FunctionDescriptor.of(POINTER, POINTER));
  }

  private void unwrap(int errorCode) {
    unwrap(errorCode, null);
  }

  private void unwrap(int errorCode, @Nullable String message) {
    switch (errorCode) {
      case OK:
        return;
      case ERROR:
      case INTERNAL:
      case PERM:
      case ABORT:
      case BUSY:
      case LOCKED:
      case READONLY:
      case INTERRUPT:
      case IOERR:
      case CORRUPT:
      case NOTFOUND:
      case FULL:
      case CANTOPEN:
      case PROTOCOL:
      case EMPTY:
      case SCHEMA:
      case TOOBIG:
      case CONSTRAINT:
      case MISMATCH:
      case MISUSE:
      case NOLFS:
      case AUTH:
      case FORMAT:
      case RANGE:
      case NOTADB:
      case NOTICE:
      case WARNING:
        throw new Exception(errorCode, message);
      case NOMEM:
        throw new OutOfMemoryError();
      default:
        throw new AssertionError(String.valueOf(errorCode));
    }
  }

  public String libversion() {
    try {
      var result = (MemorySegment) sqlite3_libversion.invokeExact();
      return result.getString(0);
    } catch (Error | RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new AssertionError(e);
    }
  }

  public String sourceid() {
    try {
      var result = (MemorySegment) sqlite3_sourceid.invokeExact();
      return result.getString(0);
    } catch (Error | RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new AssertionError(e);
    }
  }

  public void config(int option, int value) {
    try {
      unwrap((int) sqlite3_config.invokeExact(option, value));
    } catch (Error | RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new AssertionError(e);
    }
  }

  public void initialize() {
    try {
      unwrap((int) sqlite3_initialize.invokeExact());
    } catch (Error | RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new AssertionError(e);
    }
  }

  public void shutdown() {
    try {
      unwrap((int) sqlite3_shutdown.invokeExact());
    } catch (Error | RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new AssertionError(e);
    }
  }

  public void free(MemorySegment pointer) {
    try {
      sqlite3_free.invokeExact(pointer);
    } catch (Error | RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new AssertionError(e);
    }
  }

  public Db open(String filename, int openFlags, @Nullable String vfs) {
    try (var confined = Arena.ofConfined()) {
      var filenamePtr = confined.allocateFrom(filename);
      var vfsPtr = vfs == null ? NULL : confined.allocateFrom(vfs);
      var resultPtr = confined.allocate(POINTER);
      try {
        unwrap((int) sqlite3_open_v2.invokeExact(filenamePtr, resultPtr, openFlags, vfsPtr));
        return new Db(resultPtr.get(POINTER, 0));
      } catch (Throwable e) {
        var result = resultPtr.get(POINTER, 0);
        if (!result.equals(NULL)) {
          new Db(result).close();
        }
        throw e;
      }
    } catch (Error | RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new AssertionError(e);
    }
  }

  public String errorString(int errorCode) {
    try {
      var result = (MemorySegment) sqlite3_errstr.invokeExact(errorCode);
      return result.getString(0);
    } catch (Error | RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new AssertionError(e);
    }
  }

  public static final class Config {
    public static final int SINGLETHREAD = 1;
    public static final int MULTITHREAD = 2;
    public static final int SERIALIZED = 3;
    public static final int URI = 17;

    private Config() {
    }
  }

  public static final class Open {
    public static final int READONLY = 1;
    public static final int READWRITE = 2;
    public static final int CREATE = 4;
    public static final int URI = 64;
    public static final int MEMORY = 128;
    public static final int NOMUTEX = 32768;
    public static final int FULLMUTEX = 65536;
    public static final int SHAREDCACHE = 131072;
    public static final int PRIVATECACHE = 262144;
    public static final int NOFOLLOW = 16777216;
    public static final int EXRESCODE = 33554432;

    private Open() {
    }
  }

  public static final class Prepare {
    public static final int PERSISTENT = 1;
    public static final int NORMALIZE = 2;
    public static final int NO_VTAB = 4;
    public static final int DONT_LOG = 16;

    private Prepare() {
    }
  }

  public final class Exception extends RuntimeException {
    public final int code;

    public Exception(int code, @Nullable String message) {
      super(errorString(code) + (message == null ? "" : ": " + message)
      );
      this.code = code;
    }
  }

  public final class Db implements Closeable {
    private final MemorySegment ptr;

    public Db(MemorySegment ptr) {
      this.ptr = ptr;
    }

    public long changes() {
      try {
        return (long) sqlite3_changes64.invokeExact(ptr);
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    @Override
    public void close() {
      try {
        int errorCode = (int) sqlite3_close.invokeExact(ptr);
        if (errorCode != OK)
          unwrap(errorCode, errorMessage().orElse(null));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public void close2() {
      try {
        int errorCode = (int) sqlite3_close_v2.invokeExact(ptr);
        if (errorCode != OK)
          unwrap(errorCode, errorMessage().orElse(null));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public void exec(String sql) {
      try (var confined = Arena.ofConfined()) {
        var sqlPtr = confined.allocateFrom(sql);
        var errmsgPtr = confined.allocate(POINTER);
        int errorCode = (int) sqlite3_exec.invokeExact(ptr, sqlPtr, NULL, NULL, errmsgPtr);
        if (errorCode != OK) {
          var errmsg = errmsgPtr.get(POINTER, 0);
          if (errmsg.equals(NULL)) {
            unwrap(errorCode);
          } else {
            String message = errmsg.getString(0);
            sqlite3_free.invokeExact(errmsg);
            unwrap(errorCode, message);
          }
        }
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public int errorCode() {
      try {
        return (int) sqlite3_errcode.invokeExact(ptr);
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public Optional<String> errorMessage() {
      try {
        var result = (MemorySegment) sqlite3_errmsg.invokeExact(ptr);
        return result.equals(NULL) ? Optional.empty() : Optional.of(result.getString(0));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public Stmt prepare(String sql, int prepareFlags) {
      try (var confined = Arena.ofConfined()) {
        var sqlPtr = confined.allocateFrom(sql);
        var resultPtr = confined.allocate(POINTER);
        int errorCode = (int) sqlite3_prepare_v3.invokeExact(
          ptr, sqlPtr, (int) sqlPtr.byteSize() - 1, prepareFlags, resultPtr, NULL
        );
        if (errorCode != OK)
          unwrap(errorCode, errorMessage().orElse(null));
        return new Stmt(resultPtr.get(POINTER, 0));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public MemorySegment serialize(String schema) {
      try (var confined = Arena.ofConfined()) {
        var schemaPtr = confined.allocateFrom(schema);
        var sizePtr = confined.allocate(INT);
        var result = (MemorySegment) sqlite3_serialize.invokeExact(ptr, schemaPtr, sizePtr, 0);
        return result.reinterpret(sizePtr.get(INT, 0)).asReadOnly();
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    @Override
    public boolean equals(Object it) {
      return it instanceof Db v && ptr.equals(v.ptr);
    }

    @Override
    public int hashCode() {
      return ptr.hashCode();
    }
  }

  public final class Stmt implements Closeable {
    private final MemorySegment ptr;

    public Stmt(MemorySegment ptr) {
      this.ptr = ptr;
    }

    public void bindDouble(int index1, double value) {
      try {
        unwrap((int) sqlite3_bind_double.invokeExact(ptr, index1, value));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public void bindBlob(int index1, byte[] value) {
      try (var confined = Arena.ofConfined()) {
        var valuePtr = confined.allocateFrom(JAVA_BYTE, value);
        unwrap((int) sqlite3_bind_blob.invokeExact(ptr, index1, valuePtr, value.length, MemorySegment.ofAddress(-1)));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public void bindInt64(int index1, long value) {
      try {
        unwrap((int) sqlite3_bind_int64.invokeExact(ptr, index1, value));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public void bindNull(int index1) {
      try {
        unwrap((int) sqlite3_bind_null.invokeExact(ptr, index1));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public void bindText(int index1, String value) {
      try (var confined = Arena.ofConfined()) {
        var valuePtr = confined.allocateFrom(value);
        unwrap((int) sqlite3_bind_text.invokeExact(ptr, index1, valuePtr, (int) valuePtr.byteSize() - 1, MemorySegment.ofAddress(-1)));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public int bindParameterCount() {
      try {
        return (int) sqlite3_bind_parameter_count.invokeExact(ptr);
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public void clearBindings() {
      try {
        unwrap((int) sqlite3_clear_bindings.invokeExact(ptr));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public int columnCount() {
      try {
        return (int) sqlite3_column_count.invokeExact(ptr);
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public boolean step() {
      try {
        int result = (int) sqlite3_step.invokeExact(ptr);
        return switch (result) {
          case ROW -> true;
          case DONE -> false;
          default -> {
            unwrap(result, db().errorMessage().orElse(null));
            throw new AssertionError();
          }
        };
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public int dataCount() {
      try {
        return (int) sqlite3_data_count.invokeExact(ptr);
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public double columnDouble(int index) {
      try {
        return (double) sqlite3_column_double.invokeExact(ptr, index);
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public Optional<byte[]> columnBlob(int index) {
      try {
        var result = (MemorySegment) sqlite3_column_blob.invokeExact(ptr, index);
        return result.equals(NULL) ? Optional.empty() : Optional.of(result.reinterpret(columnBytes(index)).toArray(JAVA_BYTE));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public long columnInt64(int index) {
      try {
        return (long) sqlite3_column_int64.invokeExact(ptr, index);
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public Optional<String> columnText(int index) {
      try {
        MemorySegment result = (MemorySegment) sqlite3_column_text.invokeExact(ptr, index);
        return result.equals(NULL) ? Optional.empty() : Optional.of(result.getString(0));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public int columnBytes(int index) {
      try {
        return (int) sqlite3_column_bytes.invokeExact(ptr, index);
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    @Override
    public void close() {
      try {
        unwrap((int) sqlite3_finalize.invokeExact(ptr));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public void reset() {
      try {
        unwrap((int) sqlite3_reset.invokeExact(ptr));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    public Db db() {
      try {
        return new Db((MemorySegment) sqlite3_db_handle.invokeExact(ptr));
      } catch (Error | RuntimeException e) {
        throw e;
      } catch (Throwable e) {
        throw new AssertionError(e);
      }
    }

    @Override
    public boolean equals(Object it) {
      return it instanceof Db v && ptr.equals(v.ptr);
    }

    @Override
    public int hashCode() {
      return ptr.hashCode();
    }
  }
}
