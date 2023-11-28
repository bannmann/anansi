package dev.bannmann.anansi.core;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.github.bannmann.labs.core.ObjectExtras;
import com.github.mizool.core.Identifier;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CheckReturnValue;

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
            .timestamp(OffsetDateTime.now())
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
            .anyMatch(rootPackage -> frameData.getMethodName()
                .startsWith(rootPackage + "."));
    }

    private boolean isActualSourceCode(FrameData frameData)
    {
        /*
         * Examples:
         * com.example.ScopedRunner$Proxy$_$$_WeldSubclass.run(Unknown Source)
         * com.example.ScopedRunner$Proxy$_$$_WeldClientProxy.run(Unknown Source)
         */
        return !frameData.getMethodName()
            .contains("$Proxy$");
    }

    private List<FrameData> collectRelevantFrames(List<Throwable> causeAndConsequences)
    {
        ImmutableList<FrameData> rootCauseFrames = getFramesFromRootCause(causeAndConsequences.get(0));

        var result = ImmutableList.<FrameData>builder();
        result.addAll(rootCauseFrames);

        List<Throwable> consequences = causeAndConsequences.subList(1, causeAndConsequences.size());
        FrameData previousEntryFrame = getLastElement(rootCauseFrames);
        for (Throwable throwable : consequences)
        {
            List<FrameData> currentFrames = Arrays.stream(throwable.getStackTrace())
                .map(FrameData::from)
                .filter(this::isRelevant)
                .collect(Collectors.toList());

            FrameData currentEntryFrame = null;
            if (!currentFrames.isEmpty())
            {
                currentEntryFrame = getLastElement(currentFrames);

                /*
                 * If the previous exception A and the current one, B (with B.getCause() == A), don't share their entry
                 * frame, this means that A is in another thread. As its frames are likely not sufficient for debugging,
                 * we add those of B.
                 */
                if (!currentEntryFrame.equals(previousEntryFrame))
                {
                    result.addAll(currentFrames);
                }
            }

            previousEntryFrame = currentEntryFrame;
        }

        return result.build();
    }

    private ImmutableList<FrameData> getFramesFromRootCause(Throwable throwable)
    {
        var result = ImmutableList.<FrameData>builder();

        StackTraceElement[] stackTrace = throwable.getStackTrace();

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

    private <E> E getLastElement(List<E> list)
    {
        return list.get(list.size() - 1);
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

    private FrameData obtainLocation(List<FrameData> frames)
    {
        return frames.stream()
            .filter(this::isRelevant)
            .findFirst()
            .orElse(null);
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
            .filter(Objects::nonNull)
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
            .collect(Collectors.toList());
    }
}
