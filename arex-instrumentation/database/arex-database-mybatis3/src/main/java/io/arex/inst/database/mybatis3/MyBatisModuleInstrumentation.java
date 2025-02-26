package io.arex.inst.database.mybatis3;

import io.arex.foundation.api.ModuleInstrumentation;
import io.arex.foundation.api.ModuleDescription;
import io.arex.foundation.api.TypeInstrumentation;
import com.google.auto.service.AutoService;

import java.util.List;

import static java.util.Collections.singletonList;

@AutoService(ModuleInstrumentation.class)
public class MyBatisModuleInstrumentation extends ModuleInstrumentation {
    public MyBatisModuleInstrumentation() {
        super("mybatis-v3", ModuleDescription.builder()
                .name("mybatis").supportFrom(3, 0).build());
    }

    @Override
    public List<TypeInstrumentation> instrumentationTypes() {
        return singletonList(new ExecutorInstrumentation());
    }
}
