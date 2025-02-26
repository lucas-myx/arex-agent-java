package io.arex.inst.database.hibernate;

import io.arex.foundation.context.ContextManager;
import io.arex.foundation.context.RepeatedCollectManager;
import io.arex.foundation.model.MockResult;
import io.arex.foundation.util.AsyncHttpClientUtil;
import io.arex.inst.database.common.DatabaseExtractor;
import org.hibernate.HibernateException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class AbstractEntityPersisterInstrumentationTest {
    static AbstractEntityPersisterInstrumentation target = null;

    @BeforeAll
    static void setUp() {
        target = new AbstractEntityPersisterInstrumentation();
        Mockito.mockStatic(ContextManager.class);
        Mockito.mockStatic(RepeatedCollectManager.class);
        Mockito.mockStatic(AsyncHttpClientUtil.class);
    }

    @AfterAll
    static void tearDown() {
        target = null;
        Mockito.clearAllCaches();
    }

    @Test
    void typeMatcher() {
        assertNotNull(target.typeMatcher());
    }

    @Test
    void methodAdvices() {
        assertNotNull(target.methodAdvices());
    }

    @Test
    void adviceClassNames() {
        assertNotNull(target.adviceClassNames());
    }

    @Test
    void onInsertEnter() {
        Mockito.when(ContextManager.needRecordOrReplay()).thenReturn(true);
        Mockito.when(ContextManager.needReplay()).thenReturn(true);
        assertTrue(AbstractEntityPersisterInstrumentation.InsertAdvice.onEnter(null, null, null, null, null));
    }

    @ParameterizedTest
    @MethodSource("onInsertExitCase")
    void onInsertExit(Runnable mocker, HibernateException replayException, HibernateException exception, Predicate<HibernateException> predicate) {
        mocker.run();
        DatabaseExtractor extractor = Mockito.mock(DatabaseExtractor.class);
        Serializable serializable = Mockito.mock(Serializable.class);
        AbstractEntityPersisterInstrumentation.InsertAdvice.onExit(null, replayException, exception, MockResult.of(serializable), extractor);
        assertTrue(predicate.test(replayException));
    }

    static Stream<Arguments> onInsertExitCase() {
        Runnable emptyMocker = () -> {};
        Runnable exitAndValidate = () -> {
            Mockito.when(RepeatedCollectManager.exitAndValidate()).thenReturn(true);
            Mockito.when(ContextManager.needReplay()).thenReturn(true);
        };
        Runnable needRecord = () -> {
            Mockito.when(ContextManager.needReplay()).thenReturn(false);
            Mockito.when(ContextManager.needRecord()).thenReturn(true);
        };
        Predicate<HibernateException> predicate1 = Objects::isNull;
        Predicate<HibernateException> predicate2 = Objects::nonNull;
        return Stream.of(
                arguments(emptyMocker, null, null, predicate1),
                arguments(exitAndValidate, new HibernateException(""), null, predicate2),
                arguments(emptyMocker, null, null, predicate1),
                arguments(needRecord, null, new HibernateException(""), predicate1),
                arguments(emptyMocker, null, null, predicate1)
        );
    }

    @ParameterizedTest
    @MethodSource("onUpdateOrInsertEnterCase")
    void onUpdateOrInsertEnter(Runnable mocker, MockResult mockResult, Predicate<Integer> predicate) {
        try (MockedConstruction<DatabaseExtractor> mocked = Mockito.mockConstruction(DatabaseExtractor.class, (mock, context) -> {
            Mockito.when(mock.replay()).thenReturn(mockResult);
        })) {
            mocker.run();
            int result = AbstractEntityPersisterInstrumentation.UpdateOrInsertAdvice.onEnter(null, null, null);
            assertTrue(predicate.test(result));
        }
    }

    static Stream<Arguments> onUpdateOrInsertEnterCase() {
        Runnable needReplay = () -> {
            Mockito.when(ContextManager.needReplay()).thenReturn(true);
        };
        Predicate<Integer> predicate1 = result -> result == 0;
        Predicate<Integer> predicate2 = result -> result == 1;
        return Stream.of(
                arguments(needReplay, MockResult.of("mock"), predicate1),
                arguments(needReplay, null, predicate2)
        );
    }

    @Test
    void onUpdateOrInsertExit() {
        Mockito.when(ContextManager.needRecord()).thenReturn(true);
        Mockito.when(RepeatedCollectManager.exitAndValidate()).thenReturn(true);
        AbstractEntityPersisterInstrumentation.UpdateOrInsertAdvice.onExit(null, null, new HibernateException(""));
        assertDoesNotThrow(() -> AbstractEntityPersisterInstrumentation.UpdateOrInsertAdvice.onExit(null, null, null));
    }
}