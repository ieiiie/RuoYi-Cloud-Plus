package com.ym.agriculture.farming.farmwork.model.constants;

/**
 * stask 农事字典节点类型。
 *
 * @author ym-cloud
 */
public enum FarmWorkNodeType {

    /**
     * 农事分类，根节点，{@code parent_id = 0}。
     */
    CATEGORY,

    /**
     * 农事项目，归属于某个分类，供任务派发选择。
     */
    ITEM;

    /**
     * 判断是否为分类节点。
     *
     * @param value 节点类型字符串
     * @return 是否为分类
     */
    public static boolean isCategory(String value) {
        return CATEGORY.name().equals(value);
    }

    /**
     * 判断是否为项目节点。
     *
     * @param value 节点类型字符串
     * @return 是否为项目
     */
    public static boolean isItem(String value) {
        return ITEM.name().equals(value);
    }
}
