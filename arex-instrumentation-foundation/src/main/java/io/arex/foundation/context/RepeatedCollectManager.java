package io.arex.foundation.context;

import io.arex.agent.bootstrap.ctx.ArexThreadLocal;
import io.arex.agent.bootstrap.internal.CallDepth;

/**
 * Avoid collecting data multiple times on the call chain
 */
public class RepeatedCollectManager {

    public static void enter() {
        CallDepth callDepth = Context.get(true);
        if (callDepth != null) {
            callDepth.getAndIncrement();
        }
    }

    public static boolean exitAndValidate() {
        CallDepth callDepth = Context.get();
        if (callDepth == null) {
            return true;
        }

        if (callDepth.decrementAndGet() <= 0) {
            Context.remove();
            return true;
        }
        return false;
    }

    static class Context {
        private static final ArexThreadLocal<CallDepth> SCOPE_TL = new ArexThreadLocal<>();

        private static CallDepth get() {
            return get(false);
        }

        public static CallDepth get(boolean createIfAbsent) {
            CallDepth depth = SCOPE_TL.get();
            if (depth == null && createIfAbsent && ContextManager.needRecord()) {
                depth = CallDepth.simple();
                SCOPE_TL.set(depth);
            }
            return depth;
        }

        public static void remove() {
            SCOPE_TL.remove();
        }
    }
}
