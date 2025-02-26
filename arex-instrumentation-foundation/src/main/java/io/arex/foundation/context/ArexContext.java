package io.arex.foundation.context;


import io.arex.foundation.util.StringUtil;

import java.util.*;

public class ArexContext {
    private final String caseId;
    private final String replayId;
    private final long createTime;
    private final SequenceProvider sequence;
    private final List<Integer> methodSignatureHashList = new ArrayList<>();
    private final Map<String, Object> cacheMap = new WeakHashMap<>();
    private Map<String, Set<String>> excludeMockTemplate;

    public String getCaseId() {
        return this.caseId;
    }

    public String getReplayId() {
        return this.replayId;
    }

    public long getCreateTime() {
        return createTime;
    }

    private ArexContext(String caseId, String replayId) {
        this.createTime = System.currentTimeMillis();
        this.caseId = caseId;
        this.sequence = new SequenceProvider();
        this.replayId = replayId;
    }

    public boolean isReplay() {
        return StringUtil.isNotEmpty(this.replayId);
    }

    public void add(String key, String value) {

    }

    public String get(String key) {
        return null;
    }

    public int calculateSequence(String target) {
        return StringUtil.isEmpty(target) ? 0 : sequence.get(target);
    }

    public int calculateSequence() {
        return 0;
    }

    public static ArexContext of(String caseId) {
        return of(caseId, null);
    }

    public static ArexContext of(String caseId, String replayId) {
        return new ArexContext(caseId, replayId);
    }

    public List<Integer> getMethodSignatureHashList() {
        return methodSignatureHashList;
    }

    public Map<String, Object> getCacheMap() {
        return cacheMap;
    }
    public Map<String, Set<String>> getExcludeMockTemplate() {
        return excludeMockTemplate;
    }

    public void setExcludeMockTemplate(Map<String, Set<String>> excludeMockTemplate) {
        this.excludeMockTemplate = excludeMockTemplate;
    }
    public void clear() {
        methodSignatureHashList.clear();
        cacheMap.clear();
        sequence.clear();
        if (excludeMockTemplate != null) {
            excludeMockTemplate.clear();
        }
    }
}
