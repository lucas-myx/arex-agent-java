package io.arex.foundation.model;

public class MockResult {
    private final boolean ignoreMockResult;
    private final Object mockResult;

    private MockResult(boolean ignoreMockResult, Object mockResult) {
        this.ignoreMockResult = ignoreMockResult;
        this.mockResult = mockResult;
    }

    public boolean isIgnoreMockResult() {
        return ignoreMockResult;
    }

    public boolean notIgnoreMockResult() {
        return !isIgnoreMockResult();
    }

    public Object getMockResult() {
        return mockResult;
    }

    public static MockResult of(boolean ignoreMockResult, Object mockResult) {
        return new MockResult(ignoreMockResult, mockResult);
    }

    public static MockResult of(Object mockResult) {
        return of(false, mockResult);
    }
}
