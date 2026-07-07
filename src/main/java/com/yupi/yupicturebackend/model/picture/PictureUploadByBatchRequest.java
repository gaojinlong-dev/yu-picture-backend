package com.yupi.yupicturebackend.model.picture;

import lombok.Data;

/**
 * ClassName: PictureUploadByBatchRequest
 * Package: com.yupi.yupicturebackend.model.picture
 * Description:
 *
 * @Author gjl
 * @Create 2026/7/7 22:25
 * @Version 1.0
 */
@Data
public class PictureUploadByBatchRequest {
    /**
     * 搜索词
     */
    private  String  searchChtext;

    /**
     * 抓取数量
     */
    private  Integer count = 10;

    /**
     *图片名称
     */
    private String namePrefix;
}
