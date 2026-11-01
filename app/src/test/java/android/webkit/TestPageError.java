package android.webkit;

/** Package-local framework constructor is intentional in this test fixture. */
public final class TestPageError extends WebResourceError {
    private final int code;
    private final CharSequence message;
    public TestPageError(int code, CharSequence message) { this.code = code; this.message = message; }
    @Override public int getErrorCode() { return code; }
    @Override public CharSequence getDescription() { return message; }
}
