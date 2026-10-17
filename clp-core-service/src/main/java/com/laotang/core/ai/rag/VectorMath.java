package com.laotang.core.ai.rag;

/**
 * 向量计算。
 */
public final class VectorMath {

    private static final double EPSILON = 1e-9;

    private VectorMath() {
    }

    /**
     * 余弦相似度,范围 [-1, 1]。
     */
    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB) + EPSILON);
    }
}
