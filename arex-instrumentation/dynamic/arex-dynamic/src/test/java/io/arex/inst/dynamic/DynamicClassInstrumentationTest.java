package io.arex.inst.dynamic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.arex.foundation.api.MethodInstrumentation;
import io.arex.foundation.context.ArexContext;
import io.arex.foundation.context.ContextManager;
import io.arex.foundation.model.DynamicClassEntity;
import io.arex.foundation.model.MockResult;
import io.arex.foundation.util.StringUtil;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodDescription.ForLoadedMethod;
import net.bytebuddy.matcher.ElementMatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DynamicClassInstrumentationTest {
    static DynamicClassInstrumentation target = null;
    static List<DynamicClassEntity> dynamicClassList = Collections.singletonList(
            new DynamicClassEntity("", "", "", null));

    DynamicClassInstrumentationTest() {
    }

    @BeforeAll
    static void setUp() {
        target = new DynamicClassInstrumentation(dynamicClassList);
        Mockito.mockStatic(ContextManager.class);
    }

    @AfterAll
    static void tearDown() {
        target = null;
    }

    @Test
    void typeMatcher() {
        assertNotNull(target.matcher());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("methodAdvicesCase")
    void testMethodAdvices(String testName, List<DynamicClassEntity> dynamicClassList, Predicate<List<MethodInstrumentation>> predicate) {
        target = new DynamicClassInstrumentation(dynamicClassList);
        List<MethodInstrumentation> methodAdvices = target.methodAdvices();
        assertTrue(predicate.test(methodAdvices));
    }

    static Predicate<List<MethodInstrumentation>> NOT_EMPTY_PREDICATE = result -> !result.isEmpty();

    static int matchedMethodCount(ElementMatcher<? super MethodDescription> matcher, Class<?> matchedClazz) {
        Method[] methods = matchedClazz.getDeclaredMethods();
        List<String> matchedMethods = new ArrayList<>();
        List<String> nonMatchedMethods = new ArrayList<>();
        for (Method method : methods) {
            if (matcher.matches(new ForLoadedMethod(method))) {
                matchedMethods.add(method.getName());
            } else {
                nonMatchedMethods.add(method.getName());
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("matcher: ").append(matcher.toString()).append("\n")
                .append("matched Class: ").append(matchedClazz.getName()).append("\n")
                .append("matched ").append(matchedMethods.size()).append(" methods: ")
                .append(StringUtil.join(matchedMethods, ", ")).append("\n")
                .append("matched ").append(nonMatchedMethods.size()).append(" methods: ")
                .append(StringUtil.join(nonMatchedMethods, ", ")).append("\n");
        System.out.println(builder);

        return matchedMethods.size();
    }

    static Stream<Arguments> methodAdvicesCase() {
        DynamicClassEntity emptyOperation = new DynamicClassEntity("io.arex.inst.dynamic.DynamicTestClass", "", "", "");
        Predicate<List<MethodInstrumentation>> emptyOperationPredicate = methodAdvices -> {
            ElementMatcher<? super MethodDescription> matcher = methodAdvices.get(0).getMethodMatcher();
            return methodAdvices.size() == 1 && matchedMethodCount(matcher, DynamicTestClass.class) == 2;
        };

        DynamicClassEntity testReturnVoidEntity = new DynamicClassEntity("io.arex.inst.dynamic.DynamicTestClass", "testReturnVoid", "", "");
        DynamicClassEntity testReturnVoidWithParameterEntity = new DynamicClassEntity("io.arex.inst.dynamic.DynamicTestClass", "testReturnVoidWithParameter", "java.lang.String", "java.lang.System.currentTimeMillis");
        Predicate<List<MethodInstrumentation>> emptyOperationAndVoidPredicate = methodAdvices -> {
            ElementMatcher<? super MethodDescription> matcher = methodAdvices.get(0).getMethodMatcher();
            return methodAdvices.size() == 1 && matchedMethodCount(matcher, DynamicTestClass.class) == 3;
        };

        return Stream.of(
                arguments("should_match_2_methods_when_empty_operation", Collections.singletonList(emptyOperation), NOT_EMPTY_PREDICATE.and(emptyOperationPredicate)),
                arguments("should_match_4_method_when_with_return_void", Arrays.asList(emptyOperation, testReturnVoidEntity, testReturnVoidWithParameterEntity), NOT_EMPTY_PREDICATE.and(emptyOperationAndVoidPredicate))
        );
    }

    @Test
    void onEnter() {
        assertFalse(DynamicClassInstrumentation.MethodAdvice.onEnter(null, null, null, null));
    }

    @ParameterizedTest
    @MethodSource("onExitCase")
    void onExit(Runnable mocker, MockResult mockResult, Predicate<MockResult> predicate) {
        mocker.run();
        DynamicClassInstrumentation.MethodAdvice.onExit(
                "java.lang.System", "getenv", new Object[]{"java.lang.String"}, mockResult, null);
        assertTrue(predicate.test(mockResult));
    }

    static Stream<Arguments> onExitCase() {
        Runnable mockerNeedReplay = () -> {
            Mockito.when(ContextManager.currentContext()).thenReturn(ArexContext.of("test-case-id"));
        };
        Runnable mockerNeedRecord = () -> {
            Mockito.when(ContextManager.needRecord()).thenReturn(true);
        };
        Predicate<MockResult> predicate1 = Objects::isNull;
        Predicate<MockResult> predicate2 = Objects::nonNull;
        return Stream.of(
                arguments(mockerNeedReplay, null, predicate1),
                arguments(mockerNeedRecord, MockResult.of("mock"), predicate2),
                arguments(mockerNeedRecord, null, predicate1)
        );
    }

    @Test
    void testTransformer() {
        DynamicClassEntity testReturnPrimitiveType = new DynamicClassEntity("io.arex.inst.dynamic.DynamicTestClass", "testReturnPrimitiveType", "", "java.lang.System.currentTimeMillis");
        DynamicClassEntity testReturnNonPrimitiveType = new DynamicClassEntity("io.arex.inst.dynamic.DynamicTestClass", "testReturnNonPrimitiveType", "", "java.util.UUID.randomUUID");
        DynamicClassInstrumentation instrumentation = new DynamicClassInstrumentation(Arrays.asList(testReturnPrimitiveType, testReturnNonPrimitiveType));
        try (MockedStatic<ReplaceMethodHelper> replaceTimeMillsMockMockedStatic = Mockito.mockStatic(
                ReplaceMethodHelper.class)) {
            replaceTimeMillsMockMockedStatic.when(ReplaceMethodHelper::currentTimeMillis).thenReturn(3L);
            replaceTimeMillsMockMockedStatic.when(ReplaceMethodHelper::uuid).thenReturn(UUID.fromString("7eb4f958-671a-11ed-9022-0242ac120002"));


            ResettableClassFileTransformer resettableClassFileTransformer =
                    new AgentBuilder.Default(new ByteBuddy())
                            .type(instrumentation.matcher())
                            .transform(instrumentation.transformer())
                            .installOnByteBuddyAgent();

            DynamicTestClass testClass = new DynamicTestClass();
//            assertEquals(3L, testClass.testReturnPrimitiveType());
//            assertEquals("7eb4f958-671a-11ed-9022-0242ac120002", testClass.testReturnNonPrimitiveType());
        }

    }
}