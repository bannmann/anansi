package dev.bannmann.anansi.core;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.Nullable;

import com.github.mizool.core.Identifier;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CheckReturnValue;
import dev.bannmann.anansi.api.ContextDataProvider;
import dev.bannmann.anansi.api.ContextSupport;
import dev.bannmann.anansi.api.Fingerprintable;
import dev.bannmann.anansi.api.Fingerprinter;
import dev.bannmann.anansi.api.FrameData;
import dev.bannmann.anansi.api.StackFrame;
import dev.bannmann.labs.annotations.SuppressWarningsRationale;
import dev.bannmann.labs.core.ObjectExtras;

@RequiredArgsConstructor
@Slf4j
public final class Anansi
{
    @Data
    private static class ExtraFingerprintContents
    {
        private Map<String, Object> data = new HashMap<>();
        private List<FrameData> frames = Collections.emptyList();
    }

    private static final FingerprinterCatalog FINGERPRINTERS = new FingerprinterCatalog();
    private static final ContextDataProviderCatalog CONTEXT_DATA_PROVIDERS = new ContextDataProviderCatalog();
    private static final ThreadLocal<ThrowableContextMap> THROWABLE_CONTEXT_DATA = ThreadLocal.withInitial(
        ThrowableContextMap::new);
    private static final ThreadLocal<Set<CallerContext>> CALLER_CONTEXTS = ThreadLocal.withInitial(LinkedHashSet::new);

    @CheckReturnValue
    public static FluentContext withContext()
    {
        return new FluentContext();
    }

    static CallerContext createAndRegisterContext()
    {
        Set<CallerContext> contexts = CALLER_CONTEXTS.get();
        contexts.removeIf(CallerContext::isFailed);

        CallerContext newContext = new CallerContext(Anansi::removeContext);
        contexts.add(newContext);
        return newContext;
    }

    private static void removeContext(CallerContext callerContext)
    {
        CALLER_CONTEXTS.get()
            .remove(callerContext);
    }

    @CheckReturnValue
    public static FluentAttacher attachingTo(@NonNull Throwable throwable)
    {
        ThrowableContextMap throwableContextMap = THROWABLE_CONTEXT_DATA.get();
        return new FluentAttacher((k, v) -> throwableContextMap.put(throwable, k, v));
    }

    public static Map<String, Object> getCurrentContextData()
    {
        HashMap<String, Object> result = new HashMap<>();

        Set<CallerContext> callerContexts = CALLER_CONTEXTS.get();
        for (var callerContext : callerContexts)
        {
            result.putAll(callerContext.getData());
        }

        return result;
    }

    private final @NonNull String applicationBuildInfo;
    private final @NonNull List<String> applicationPackageRoots;

    private final @NonNull IncidentStore incidentStore;

    /**
     * @param throwable the throwable that caused the incident
     * @param severity the desired severity
     *
     * @return an {@code Optional} containing the recorded incident, or an empty {@code Optional} if the incident could not be recorded
     */
    public Optional<Incident> recordIncident(@NonNull Throwable throwable, @NonNull Severity severity)
    {
        try
        {
            var incident = createIncident(throwable, severity);
            var storableFingerprint = StorableFingerprint.from(incident);
            incidentStore.store(incident, storableFingerprint);
            return Optional.of(incident);
        }
        catch (RuntimeException e)
        {
            log.warn("Exception occurred while attempting to record incident for given {} with message \"{}\"",
                throwable.getClass()
                    .getName(),
                throwable.getMessage(),
                e);
            return Optional.empty();
        }
    }

    private Incident createIncident(Throwable throwable, Severity severity)
    {
        List<Throwable> throwables = toCauseAndConsequencesList(throwable);

        return Incident.builder()
            .id(Identifier.forPojo(Incident.class)
                .random())
            .timestamp(OffsetDateTime.now(ZoneId.systemDefault()))
            .fingerprintData(collectFingerprintData(throwables))
            .severity(severity)
            .contextData(collectContextData(throwables))
            .throwableDetails(collectThrowableDetails(throwables))
            .applicationBuildInfo(applicationBuildInfo)
            .build();
    }

    private List<Throwable> toCauseAndConsequencesList(Throwable wrappingThrowable)
    {
        List<Throwable> result = new ArrayList<>(Throwables.getCausalChain(wrappingThrowable));
        Collections.reverse(result);
        return Collections.unmodifiableList(result);
    }

    private FingerprintData collectFingerprintData(List<Throwable> causeAndConsequences)
    {
        Throwable cause = causeAndConsequences.get(0);

        String throwableClassName = cause.getClass()
            .getName();
        List<FrameData> relevantFrames = collectRelevantFrames(causeAndConsequences);

        ExtraFingerprintContents extraContents = collectExtraContents(causeAndConsequences);
        Map<String, Object> extraData = extraContents.getData();
        List<FrameData> extraFrames = extraContents.getFrames();
        if (shouldAppendExtraFrames(extraFrames, relevantFrames))
        {
            var combinedFrames = new ArrayList<FrameData>();
            combinedFrames.addAll(relevantFrames);
            combinedFrames.addAll(extraFrames);
            relevantFrames = combinedFrames;
        }

        FrameData location = obtainLocation(relevantFrames);

        return FingerprintData.builder()
            .throwableClassName(throwableClassName)
            .location(location)
            .relevantFrames(relevantFrames)
            .extraData(extraData)
            .build();
    }

    private boolean shouldAppendExtraFrames(List<FrameData> extraFrames, List<FrameData> relevantFrames)
    {
        if (extraFrames.isEmpty())
        {
            return false;
        }

        Optional<FrameData> topmostRelevantExtraFrame = extraFrames.stream()
            .filter(this::isRelevant)
            .reduce((first, second) -> second);
        if (topmostRelevantExtraFrame.isEmpty())
        {
            return false;
        }

        return !relevantFrames.contains(topmostRelevantExtraFrame.get());
    }

    private boolean isRelevant(FrameData frameData)
    {
        return isInsideApplication(frameData) && isActualSourceCode(frameData);
    }

    private boolean isInsideApplication(FrameData frameData)
    {
        return applicationPackageRoots.stream()
            .anyMatch(rootPackage -> frameData.getClassName()
                .startsWith(rootPackage + "."));
    }

    private boolean isActualSourceCode(FrameData frameData)
    {
        /*
         * Examples:
         * com.example.ScopedRunner$Proxy$_$$_WeldSubclass.run(Unknown Source)
         * com.example.ScopedRunner$Proxy$_$$_WeldClientProxy.run(Unknown Source)
         */
        return !frameData.getClassName()
            .contains("$Proxy$");
    }

    private List<FrameData> collectRelevantFrames(List<Throwable> causeAndConsequences)
    {
        List<FrameData> rootCauseFrames = getFramesFromRootCause(causeAndConsequences.get(0));

        if (causeAndConsequences.size() == 1)
        {
            return rootCauseFrames;
        }

        List<Throwable> remainingThrowables = causeAndConsequences.subList(1, causeAndConsequences.size());

        if (rootCauseFrames.isEmpty())
        {
            /*
             * Not sure why this could ever be the case. However, it did happen. In that case, it's useful to start our
             * relevant frames list with those of the *second* exception. This way, we apply the "topmost stack frame is
             * always relevant" rule to that exception.
             *
             * Note that apart from the relevant frames list, the incident data will still point out the original root
             * cause, especially its message.
             */
            return collectRelevantFrames(remainingThrowables);
        }

        // The regular case: combine root cause and consequence exception frames in cross-thread situations.
        return combineFrames(rootCauseFrames, remainingThrowables);
    }

    private List<FrameData> getFramesFromRootCause(Throwable throwable)
    {
        StackTraceElement[] stackTrace = throwable.getStackTrace();

        /*
         * On 2024-03-31 at 01:13:41, a downstream application encountered a root cause throwable with an empty stack
         * trace array. That caused an ArrayIndexOutOfBoundsException in this method, so Anansi only logged the
         * following:
         *
         *     Exception occurred while attempting to record incident for given
         *     com.example.TemporaryFailureException with message
         *     "java.util.concurrent.ExecutionException: java.lang.NullPointerException"
         */
        if (stackTrace.length == 0)
        {
            return List.of();
        }

        var result = ImmutableList.<FrameData>builder();

        // For the root cause, we always add the topmost stack frame even if it would not be deemed relevant.
        result.add(FrameData.from(stackTrace[0]));

        // Add remaining frames, but only those that are relevant
        Arrays.stream(stackTrace)
            .skip(1)
            .map(FrameData::from)
            .filter(this::isRelevant)
            .forEachOrdered(result::add);

        return result.build();
    }

    private ImmutableList<FrameData> combineFrames(List<FrameData> rootCauseFrames, List<Throwable> consequences)
    {
        var result = ImmutableList.<FrameData>builder();
        result.addAll(rootCauseFrames);

        FrameData previousEntryFrame = getLastElementOrNull(rootCauseFrames);
        for (Throwable throwable : consequences)
        {
            List<FrameData> currentFrames = Arrays.stream(throwable.getStackTrace())
                .map(FrameData::from)
                .filter(this::isRelevant)
                .collect(Collectors.toList());

            FrameData currentEntryFrame = null;
            if (!currentFrames.isEmpty())
            {
                currentEntryFrame = obtainLastElement(currentFrames);

                /*
                 * If the previous exception "A" and the current one "B" (with B.getCause() == A) don't share their
                 * entry frame, this means that A is in another thread. As the frames of A are likely not sufficient for
                 * debugging, we append those of B.
                 */
                if (framesDiffer(currentEntryFrame, previousEntryFrame))
                {
                    result.addAll(currentFrames);
                }
            }

            previousEntryFrame = currentEntryFrame;
        }

        return result.build();
    }

    private <E> @Nullable E getLastElementOrNull(List<E> list)
    {
        if (list.isEmpty())
        {
            return null;
        }
        return obtainLastElement(list);
    }

    private <E> E obtainLastElement(List<E> list)
    {
        return list.get(list.size() - 1);
    }

    @SuppressWarnings("NullAway")
    @SuppressWarningsRationale("NullAway claims Object.equals() requires a non-null argument")
    private boolean framesDiffer(FrameData first, @Nullable FrameData second)
    {
        return !first.equals(second);
    }

    private ExtraFingerprintContents collectExtraContents(List<Throwable> throwables)
    {
        ExtraFingerprintContents result = new ExtraFingerprintContents();

        for (Throwable throwable : throwables)
        {
            ObjectExtras.tryCast(throwable, Fingerprintable.class)
                .ifPresent(fingerprintable -> {
                    result.getData()
                        .putAll(fingerprintable.getFingerprintData());
                    result.setFrames(fingerprintable.getAdditionalFrames());
                });

            for (Fingerprinter<?> fingerprinter : FINGERPRINTERS.lookup(throwable))
            {
                try
                {
                    result.getData()
                        .putAll(fingerprinter.extractDataFromThrowable(throwable));
                    result.setFrames(fingerprinter.extractFramesFromThrowable(throwable));
                }
                catch (RuntimeException e)
                {
                    log.warn("{} failed for throwable {}",
                        fingerprinter.getClass()
                            .getName(),
                        throwable,
                        e);
                }
            }
        }
        return result;
    }

    private @Nullable FrameData obtainLocation(List<FrameData> frames)
    {
        return frames.stream()
            .filter(this::isRelevant)
            .filter(this::isEligibleIncidentLocation)
            .findFirst()
            .orElse(null);
    }

    private boolean isEligibleIncidentLocation(FrameData frameData)
    {
        try
        {
            Class<?> frameClass = Class.forName(frameData.getClassName());
            return Arrays.stream(frameClass.getDeclaredMethods())
                .filter(method -> method.getName()
                    .equals(frameData.getMethodName()))
                .flatMap(method -> Arrays.stream(method.getAnnotations()))
                .filter(StackFrame.class::isInstance)
                .map(StackFrame.class::cast)
                .map(StackFrame::incidentLocation)
                .findFirst()
                .orElse(true);
        }
        catch (ClassNotFoundException e)
        {
            log.debug("Failed to access annotations of class {}", frameData.getClassName(), e);
            return true;
        }
    }

    private Map<String, Object> collectContextData(List<Throwable> throwables)
    {
        Map<String, Object> result = new HashMap<>();
        fetchContextDataFromCallers(result);

        for (Throwable throwable : throwables)
        {
            fetchContextDataFromThrowable(result, throwable);
            fetchContextDataFromProviders(result, throwable);
            fetchContextDataAttachedToThrowable(result, throwable);
        }
        return result;
    }

    private void fetchContextDataFromCallers(Map<String, Object> result)
    {
        CALLER_CONTEXTS.get()
            .stream()
            .map(CallerContext::getData)
            .forEachOrdered(result::putAll);
    }

    private void fetchContextDataFromThrowable(Map<String, Object> result, Throwable throwable)
    {
        ObjectExtras.tryCast(throwable, ContextSupport.class)
            .map(ContextSupport::getContextData)
            .ifPresent(result::putAll);
    }

    private void fetchContextDataFromProviders(Map<String, Object> result, Throwable throwable)
    {
        for (ContextDataProvider<?> contextDataProvider : CONTEXT_DATA_PROVIDERS.lookup(throwable))
        {
            try
            {
                Map<String, Object> contextData = contextDataProvider.getContextDataForThrowable(throwable);
                result.putAll(contextData);
            }
            catch (RuntimeException e)
            {
                log.warn("{} failed for throwable {}",
                    contextDataProvider.getClass()
                        .getName(),
                    throwable,
                    e);
            }
        }
    }

    private void fetchContextDataAttachedToThrowable(Map<String, Object> result, Throwable throwable)
    {
        THROWABLE_CONTEXT_DATA.get()
            .getAll(throwable)
            .ifPresent(result::putAll);
    }

    private List<ThrowableData> collectThrowableDetails(List<Throwable> throwables)
    {
        return throwables.stream()
            .map(ThrowableData::from)
            .toList();
    }

    /**
     * @param throwable the throwable that caused the incident
     * @param severity the desired severity
     *
     * @return the incident ID, or {@code null} if the incident could not be recorded
     */
    public @Nullable String recordIncidentAndOnlyGetId(@NonNull Throwable throwable, @NonNull Severity severity)
    {
        return recordIncident(throwable, severity).map(Incident::getId)
            .map(Identifier::getValue)
            .orElse(null);
    }
}
