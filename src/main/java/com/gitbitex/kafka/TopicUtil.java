package com.gitbitex.kafka;

public class TopicUtil {
    public static String getProductTopic(String productId, String topicSuffix) {
        return productId + "_" + topicSuffix;
    }

    public static String parseProductIdFromTopic(String productTopic) {
        return productTopic.split("_")[0];
    }
}
