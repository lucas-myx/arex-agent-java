package io.arex.inst.loader;

import io.arex.foundation.api.ModuleDescription;
import io.arex.foundation.api.ModuleInstrumentation;
import io.arex.foundation.api.TypeInstrumentation;
import com.google.auto.service.AutoService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * LoaderSpringModuleInstrumentation
 *
 *
 * @date 2022/03/03
 */
@AutoService(ModuleInstrumentation.class)
public class ClassLoaderModuleInstrumentation extends ModuleInstrumentation {

    public ClassLoaderModuleInstrumentation() {
        super("class-loader");
    }

    @Override
    public List<TypeInstrumentation> instrumentationTypes() {
        return /*Arrays.asList(new ClassLoaderInstrumentation(), new AppClassLoaderInstrumentation(),
                new URLClassLoaderInstrumentation(), new WebAppClassLoaderBaseInstrumentation(),
                new ParallelWebappClassLoaderInstrumentation());*/
        Collections.singletonList(new InjectClassInstrumentation());
    }
}
