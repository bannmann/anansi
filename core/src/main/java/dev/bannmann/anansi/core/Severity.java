package dev.bannmann.anansi.core;

/**
 * Categorizes an incident's effects to the operation of the application.
 */
public enum Severity
{
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
     * Application deals with this incident by attempting to use a different approach, or tweaking the parameters it
     * uses. Use of that approach could be limited to when a problem occurs because it's more costly than the default
     * (e.g. requiring more requests, or be slower).<br>
     * <br>
     * Of course, the attempt with the different approach may fail itself, and unless yet another approach is chosen,
     * that failure would then be recorded with a severity different from {@code ADAPT}.<br>
     * <br>
     * Note: if the application simply retries the original operation without changing anything, one of
     * {@link #RETRY_NOW}, {@link #RETRY_SOON}, or {@link #RETRY_LATER} should be used instead.
     */
    ADAPT,

    /**
     * At the time of the incident, it already did not matter anymore due to a concurrent change. For example, a
     * connection run failure could be logged as obsolete because the connection was already deleted in the meantime.
     */
    OBSOLETE,

    /**
     * Application functionality is (or may be) affected in some way, but the operation is not aborted.
     */
    WARN,

    /**
     * Application sends a negative REST response to its client.
     */
    API_FAILURE,

    /**
     * Internal failure with unknown recoverability (e.g. a thread dying).
     */
    INTERNAL_FAILURE,

    /**
     * A background process (i.e. not a REST call by the user) failed in a controlled way. The application will not
     * retry on its own, but wait for user commands or other changes.
     */
    VISIBLE_FAILURE,

    /**
     * Application retries the operation immediately. Here, "immediately" is only meant in a loose way and includes
     * small delays, usually less than a minute. The point is that to the user, the effects of such a retry are hardly
     * noticeable - the operation just looks to take a bit longer than usual.
     */
    RETRY_NOW,

    /**
     * Application will retry the operation - slower than {@link #RETRY_NOW}, but earlier than {@link #RETRY_LATER}.
     */
    RETRY_SOON,

    /**
     * Application will retry the operation later. Usually, that means a range of hours or even days.
     */
    RETRY_LATER,

    /**
     * Application crashes or shuts down.
     */
    FATAL
}
