package dev.bannmann.anansi.core;

/**
 * Categorizes an incident's effects on the application.
 */
public enum Severity
{
    /**
     * Incident occurred as a consequence or during the process of adapting to another, original incident. Usually, such
     * secondary incidents do not need to be addressed on their own.<br>
     * <br>
     * Note that this is different from one exception <i>causing</i> another. In both cases, there is a causal
     * relationship between the first and the second exception: the second one would have never happened without the
     * first. The difference between the two cases is that for "A causes B", B is a direct and inevitable consequence of
     * A. In contrast, the {@code SECONDARY} severity implies that A happened (and is recorded as an incident), and then
     * an attempt to mitigate the effects of A, which could have succeeded, failed due to problem B.<br>
     * <br>
     * If the application encounters yet another problem while addressing the {@code SECONDARY} incident and records it,
     * that incident should use {@code SECONDARY}, as well.
     *
     * @see #ADAPT
     */
    SECONDARY,

    /**
     * Incident was intentionally ignored by the application as its functionality is not affected. <br>
     * <br>
     * Example use: a third-party REST API performs an action and responds with a 204, but violates the HTTP spec by
     * including a 'Transfer-Encoding' header, causing an exception in the application. While throwing the exception
     * slightly slows down the application, it does not really affect functionality.<br>
     * <br>
     * Note: if the application "ignores" the problem by switching to a different approach for the attempted operation,
     * {@link #ADAPT} should be used instead.
     */
    IGNORE,

    /**
     * At the time of the incident, it already did not matter anymore due to a concurrent change. <br>
     * <br>
     * For example, a failed run of a scheduled task could be logged as obsolete because the task was deleted in the
     * meantime.
     */
    OBSOLETE,

    /**
     * An independent background process ({@literal i.e.} unrelated to API calls by external clients) encountered a
     * permanent but harmless error. The application will not retry on its own, but wait for user commands or other
     * changes.
     *
     * @see #VISIBLE_FAILURE
     */
    VISIBLE_INFO,

    /**
     * Application retries the operation immediately. <br>
     * <br>
     * Here, "immediately" is only meant in a loose way and includes small delays, usually less than a minute. The point
     * is that to the user, the effects of such a retry are hardly noticeable - the operation just looks to take a bit
     * longer than usual.
     */
    RETRY_NOW,

    /**
     * Application will retry the operation - later than {@link #RETRY_NOW}, but earlier than {@link #RETRY_LATER}.
     */
    RETRY_SOON,

    /**
     * Application will retry the operation later. Usually, that means a range of hours or even days.
     */
    RETRY_LATER,

    /**
     * Application deals with this incident by attempting to use a different approach, or tweaking the parameters it
     * uses. Use of that approach could be limited to when a problem occurs because it's more costly than the default
     * (e.g. requiring more requests, or being slower).<br>
     * <br>
     * Of course, the attempt with the different approach may itself fail, as well. Unless yet another approach is
     * chosen, that failure would then be recorded with a severity different from {@code ADAPT}, e.g.
     * {@link #SECONDARY}.<br>
     * <br>
     * Note: if the application simply retries the original operation without changing anything, one of
     * {@link #RETRY_NOW}, {@link #RETRY_SOON}, or {@link #RETRY_LATER} should be used instead.
     */
    ADAPT,

    /**
     * Application functionality is (or may be) affected in some way, but the operation is not aborted.
     */
    WARN,

    /**
     * A harmless incident caused the application to send a negative response to the client that called the API.
     * Examples include the user changing their mind during an OAuth flow leading to an "access denied" error, or
     * when a client requests a nonexistent object but for some reason the request should be logged.<br>
     * <br>
     * If the problem is not obviously harmless, but is (or could be) caused by the application, {@link #API_FAILURE} is
     * more appropriate.
     *
     * @see #VISIBLE_INFO
     * @see #API_FAILURE
     */
    API_INFO,

    /**
     * Application sends a negative response to the client that called the API.
     */
    API_FAILURE,

    /**
     * An independent background process ({@literal i.e.} unrelated to API calls by external clients) encountered a
     * permanent error that a user is told about. The application will not retry on its own, but wait for user commands
     * or other changes.
     *
     * @see #VISIBLE_FAILURE
     */
    VISIBLE_FAILURE,

    /**
     * Internal failure with unknown recoverability ({@literal e.g.} a thread dying).
     */
    INTERNAL_FAILURE,

    /**
     * Application crashes or shuts down.
     */
    FATAL
}
