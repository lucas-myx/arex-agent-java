package io.arex.inst.time;

import static net.bytebuddy.matcher.ElementMatchers.isNative;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.arex.foundation.api.MethodInstrumentation;
import io.arex.foundation.api.TypeInstrumentation;
import java.util.List;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * TimeMachineInstrumentation
 */
public class TimeMachineInstrumentation extends TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("java.lang.System");
    }

    @Override
    public List<MethodInstrumentation> methodAdvices() {
        return null;
    }

    @Override
    public Transformer transformer() {
        return (builder, typeDescription, classLoader, module) -> {
            return builder.method(isNative().and(isStatic()).and(named("currentTimeMillis")))
                .intercept(Advice.to(TimeMachineInterceptor.class));
        };
    }
}
