package com.yupi.yupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * ClassName: FilePictureUploadImpl
 * Package: com.yupi.yupicturebackend.manager.upload
 * Description: 文件上传实现类
 *
 * @Author gjl
 * @Create 2026/5/15 22:53
 * @Version 1.0
 */
@Service
public class FilePictureUpload extends PictureuploadTemplate {
    @Override
    protected void processFile(Object inputSoure, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSoure;
        multipartFile.transferTo(file);
    }

    @Override
    protected String getOriginalFilename(Object inputSoure) {
        MultipartFile multipartFile = (MultipartFile) inputSoure;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void validPicture(Object inputSoure) {
        MultipartFile multipartFile = (MultipartFile) inputSoure;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1，校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
        // 2，校验文件后缀
        String fileSuffux = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //定义允许上传的图片格式
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffux), ErrorCode.PARAMS_ERROR, "文件格式错误");

    }
}
