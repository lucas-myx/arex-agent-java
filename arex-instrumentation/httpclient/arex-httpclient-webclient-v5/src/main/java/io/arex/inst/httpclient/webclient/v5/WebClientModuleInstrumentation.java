package io.arex.inst.httpclient.webclient.v5;

import com.google.auto.service.AutoService;
import io.arex.foundation.api.ModuleInstrumentation;
import io.arex.foundation.api.TypeInstrumentation;

import java.util.List;

import static java.util.Collections.singletonList;

@AutoService(ModuleInstrumentation.class)
public class WebClientModuleInstrumentation extends ModuleInstrumentation {
    public WebClientModuleInstrumentation() {
        super("webclient-v5");
    }

    @Override
    public List<TypeInstrumentation> instrumentationTypes() {
        return singletonList(new WebClientInstrumentation());
    }
}