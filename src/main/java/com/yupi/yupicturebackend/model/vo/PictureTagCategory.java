package com.yupi.yupicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * ClassName: PictureTagCategory 图片标签分类列表视图
 * Package: com.yupi.yupicturebackend.model.vo
 * Description:
 *
 * @Author gjl
 * @Create 2026/5/12 21:08
 * @Version 1.0
 */
@Data
public class PictureTagCategory {

    /*
    * 标签列表
     */
    private List<String> tagList;

    /*
    * 分类列表
     */
    private List<String> categoryList;
}
